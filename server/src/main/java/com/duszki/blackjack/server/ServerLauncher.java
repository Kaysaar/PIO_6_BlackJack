package com.duszki.blackjack.server;

import com.duszki.blackjack.server.Card.*;
import com.duszki.blackjack.server.Player.PlayerServerData;
import com.duszki.blackjack.shared.data.GameUpdateData;
import com.duszki.blackjack.shared.data.PlayerData;
import com.duszki.blackjack.shared.models.Card;
import com.duszki.blackjack.shared.models.Hand;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import java.io.IOException;
import java.util.*;

import com.duszki.blackjack.shared.events.*;
import com.esotericsoftware.minlog.Log;
import com.duszki.blackjack.shared.network.Network;

/**
 * Launches the server application.
 */
public class ServerLauncher {

    static public final int PORT = 5000;
    public static final boolean DEBUG = true;
    public static final int MAX_PLAYERS = 5;
    private static final int MAX_HAND_VALUE = 21;
    Server server;

    LinkedList<PlayerServerData> storedPlayerData = new LinkedList<>();
    public boolean isGameOver = false;

    private int currentPlayerCursor;
    public boolean finalRound = false;
    int currentStake = 0;

    public boolean hasGameStarted = false;

    public static final int COINS_AT_START = 2000;

    private boolean roundStarted = false;


    Shoe shoe;
    Dealer dealer;

    public ServerLauncher() throws IOException {

        if (DEBUG) {
//            Log.ERROR();
//            Log.WARN();
//            Log.INFO();
            Log.DEBUG();
//            Log.TRACE();
        }

        server = new Server();

        Network.register(server);

        addAllListeners();

        bindServer(PORT);

        server.start();

    }

    private void addAllListeners() {
        server.addListener(new Listener() {

            @Override
            public void received(Connection connection, Object object) {

                if (object instanceof JoinRequestEvent) {

                    JoinRequestEvent joinRequestEvent = (JoinRequestEvent) object;

                    JoinResponseEvent joinResponseEvent = new JoinResponseEvent();

                    if (hasGameStarted) {

                        joinResponseEvent.setSuccess(false);
                        joinResponseEvent.setMessage("Sorry, the game has already started!");
                        connection.sendTCP(joinResponseEvent);

                    } else if (joinRequestEvent.getPlayerName().isEmpty()) {

                        joinResponseEvent.setSuccess(false);
                        joinResponseEvent.setMessage("Nickname cannot be empty!");
                        connection.sendTCP(joinResponseEvent);

                    } else if (isNicknameTaken(joinRequestEvent.getPlayerName())) {

                        joinResponseEvent.setSuccess(false);
                        joinResponseEvent.setMessage("Nickname is already taken!");
                        connection.sendTCP(joinResponseEvent);

                    } else {


                        if (storedPlayerData.size() < MAX_PLAYERS) {
                            PlayerServerData serverPlayer = new PlayerServerData(connection, joinRequestEvent.getPlayerName());

                            storedPlayerData.add(serverPlayer);

                            joinResponseEvent.setMessage("Welcome " + joinRequestEvent.getPlayerName() + "!");

                            joinResponseEvent.setSuccess(true);
                        } else {
                            joinResponseEvent.setMessage("Sorry, the game is full!");
                            joinResponseEvent.setSuccess(false);
                        }
                        connection.sendTCP(joinResponseEvent);
                    }
                }
            }

        });

        server.addListener(new Listener() {

            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof RequestGameStartEvent) {
                    RequestGameStartEvent requestGameStartEvent = (RequestGameStartEvent) object;
                    hasGameStarted = true;
                    initialize_game();

                    GameStartedEvent gameStartedEvent = new GameStartedEvent();

                    server.sendToAllTCP(gameStartedEvent);

                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }


                    RequestBetEvent requestBetEvent = new RequestBetEvent();

                    server.sendToAllTCP(requestBetEvent);


                }

            }
        });

        server.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof PlaceBetEvent) {

                    PlaceBetEvent placeBetEvent = (PlaceBetEvent) object;

                    PlayerServerData currentPlayer = getPlayerByConnection(connection);

                    if (currentPlayer.getConnection() == connection) {
                        if (currentPlayer.getTokens() >= placeBetEvent.getBet() && placeBetEvent.getBet() > 0) {
                            currentPlayer.setBet(placeBetEvent.getBet());
                            currentPlayer.setTokens(currentPlayer.getTokens() - placeBetEvent.getBet());
                            currentPlayer.setBetPlaced(true);
                            server.sendToTCP(currentPlayer.getConnection().getID(), new AcceptBetEvent(true));
                        } else {
                            server.sendToTCP(currentPlayer.getConnection().getID(), new AcceptBetEvent(false));
                        }
                    }

                    if (areAllBetsPlaced()) {
                        startRound();
                    }

                }
            }
        });

        server.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof HitEvent) {

                    if (!roundStarted) return;

                    PlayerServerData currentPlayer = getPlayerByConnection(connection);

                    if (storedPlayerData.get(currentPlayerCursor) != currentPlayer) return;

                    if (currentPlayer.getConnection() == connection) {
                        if (!currentPlayer.getStand()) {
                            hit(currentPlayer);
                            sendGameUpdateToPlayers();
                        }

                    }


                }

            }
        });
        server.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof StandEvent) {

                    PlayerServerData currentPlayer = getPlayerByConnection(connection);

                    if (storedPlayerData.get(currentPlayerCursor) != currentPlayer) return;

                    if (currentPlayer.getConnection() == connection) {
                        stand(currentPlayer);
                        sendGameUpdateToPlayers();
                    }


                }

            }
        });

        server.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof DoubleDownEvent) {

                    PlayerServerData currentPlayer = getPlayerByConnection(connection);

                    if (storedPlayerData.get(currentPlayerCursor) != currentPlayer) return;

                    if (currentPlayer.getPlayerHand().getCardsInHand().size() == 2) {
                        int currTokens = currentPlayer.getTokens();
                        int currStake = currentPlayer.getBet();

                        if (currentPlayer.getConnection() == connection) {
                            if (currStake == 0 || currStake * 2 > currTokens) {
                                NotValidatedToDoEvent response = new NotValidatedToDoEvent();
                                response.setMessage("Double down event is now possible due to either not having enough money or nothing at stake yet");
                                server.sendToTCP(connection.getID(), response);
                                return;
                            }
                            doubleDown(currentPlayer);
                            sendGameUpdateToPlayers();

                        }
                    }

                    nextPlayer();


                }

            }
        });
        server.addListener(new Listener() {
            //Client info iterater thorugh hasmap by using this looop
//            for (Map.Entry<String, Integer> stringIntegerEntry : response.getPlayersSorted().entrySet()) {
//
//            }
            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof RequestCurrRankingEvent) {
                    PlayerServerData currentPlayer = getPlayerByConnection(connection);
                    if (currentPlayer.getConnection() == connection) {
                        RequestCurrRankingEvent response = (RequestCurrRankingEvent) object;

                        response.setPlayersSorted(currentRanking());

                        server.sendToAllTCP(response);
                    }
                }

            }
        });


        server.addListener(new Listener() {
            @Override
            public void disconnected(Connection connection) {
                PlayerServerData player = getPlayerByConnection(connection);

                if (currentPlayerCursor == storedPlayerData.indexOf(player)) {
                    nextPlayer();
                }

                storedPlayerData.remove(player);
                if (storedPlayerData.size() == 0) {
                    resetGame();

                }
            }
        });


    }

    private void sendGameUpdateToPlayers() {

        for (PlayerServerData player : storedPlayerData) {
            GameUpdateData gameUpdateData = getGameUpdateDataForPlayer(player);

            server.sendToTCP(player.getConnection().getID(), gameUpdateData);

        }


    }

//    private sendBalanceAtEndOfRound() {
//
//    }

    private HashMap<String, Integer> currentRanking() {
        HashMap<String, Integer> currValues = new HashMap<>();
        ArrayList<PlayerServerData> copyOfStoredData = new ArrayList<>(storedPlayerData);
        Collections.sort(copyOfStoredData, (d1, d2) -> {
            return d2.getTokens() - d1.getTokens();
        });
        for (PlayerServerData storedPlayerDatum : copyOfStoredData) {
            currValues.put(storedPlayerDatum.playerName, storedPlayerDatum.getTokens());
        }
        return currValues;
    }

    private GameUpdateData getGameUpdateDataForPlayer(PlayerServerData player) {

        GameUpdateData gameUpdateData = new GameUpdateData();
        gameUpdateData.setDealerHand(dealer.getHand());
        if (dealer.getHand().getCardsInHand().size() > 0) {
            if (roundStarted) {
                gameUpdateData.getDealerHand().getCardsInHand().get(0).setHidden(true);
            } else {
                gameUpdateData.getDealerHand().getCardsInHand().get(0).setHidden(false);
            }
        }

        gameUpdateData.getYourData().setHand(player.getPlayerHand());
        gameUpdateData.getYourData().setTokens(player.getTokens());

        for (PlayerServerData otherPlayer : storedPlayerData) {
            if (otherPlayer != player) {
                PlayerData otherPlayerData = new PlayerData();
                otherPlayerData.setHand(otherPlayer.getPlayerHand());
                otherPlayerData.setBet(otherPlayer.getBet());
                otherPlayerData.setTokens(otherPlayer.getTokens());
                gameUpdateData.getOtherPlayersData().add(otherPlayerData);
            }
        }


        return gameUpdateData;

    }


    public void bindServer(int port) {
        this.server.start();

        try {
            this.server.bind(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void resetGame() {
        hasGameStarted = false;
        storedPlayerData.clear();
        dealer.getHand().clearHand();
        finalRound = false;

    }

    public void initialize_game() {
        shoe = new Shoe();

        dealer = new Dealer();

        currentPlayerCursor = 0;

        for (PlayerServerData player : storedPlayerData) {
            player.setPlayerHand(new Hand());
            player.setTokens(COINS_AT_START);
        }

    }

    public void resetRound() {

        currentPlayerCursor = 0;
        dealer.getHand().clearHand();
        for (PlayerServerData player : storedPlayerData) {
            player.getPlayerHand().clearHand();
            player.setStand(false);
        }

        // send game update to all
        sendGameUpdateToPlayers();

        sendBetRequestToPlayers();

    }

    public void sendBetRequestToPlayers() {

        server.sendToAllTCP(new RequestBetEvent());


    }

    public void startRound() {

        roundStarted = true;

//        currentPlayerCursor = 0;
        Card card_from_deck_first;
        Card card_from_deck_second;
        for (PlayerServerData player : storedPlayerData) {
//            player.getPlayerHand().clearHand();
//            player.setStand(false);
            card_from_deck_first = shoe.getCardFromShoe();
            card_from_deck_second = shoe.getCardFromShoe();
            if (card_from_deck_second == null || card_from_deck_first == null) {
                finalRound = true;
                shoe.lastDeckRound();
                card_from_deck_first = shoe.getCardFromShoe();
                card_from_deck_second = shoe.getCardFromShoe();
            }

            player.getPlayerHand().addCard(card_from_deck_first);
            player.getPlayerHand().addCard(card_from_deck_second);
        }
        card_from_deck_first = shoe.getCardFromShoe();
        card_from_deck_second = shoe.getCardFromShoe();
        if (card_from_deck_second == null || card_from_deck_first == null) {
            finalRound = true;
            shoe.lastDeckRound();
            card_from_deck_first = shoe.getCardFromShoe();
            card_from_deck_second = shoe.getCardFromShoe();
        }
//        dealer.getHand().clearHand();
        dealer.getHand().addCard(card_from_deck_first);
        dealer.getHand().addCard(card_from_deck_second);


        // send updates to all players
        for (PlayerServerData player : storedPlayerData) {

            GameUpdateData gameUpdateData = getGameUpdateDataForPlayer(player);
            RoundStartEvent roundStartEvent = new RoundStartEvent();
            roundStartEvent.setGameUpdateData(gameUpdateData);

            server.sendToTCP(player.getConnection().getID(), roundStartEvent);

        }


        storedPlayerData.get(0).getConnection().sendTCP(new YourTurnEvent());

    }


    public void hit(PlayerServerData player) {
        System.out.println("hit");
        System.out.println(player.getPlayerHand().getHandValue());
        player.getPlayerHand().addCard(shoe.getCardFromShoe());
        if (player.getPlayerHand().getHandValue() >= MAX_HAND_VALUE) {
            stand(player);
        }
    }

    public void stand(PlayerServerData player) {
        player.setStand(true);

        // if all players have stand then dealer will play
        if (allPlayersHaveStand()) {
            roundStarted = false;
            dealerPlay();

        } else {
            nextPlayer();
        }

    }

    public void doubleDown(PlayerServerData player) {

        int currentStake = player.getBet();

        player.setTokens(player.getTokens() - currentStake);
        player.setBet(currentStake * 2);


        player.getPlayerHand().addCard(shoe.getCardFromShoe());

        sendGameUpdateToPlayers();

        stand(player);


    }

    public void nextPlayer() {
        PlayerServerData nextPlayer;

//        do {
        currentPlayerCursor = (currentPlayerCursor + 1) % storedPlayerData.size();
        nextPlayer = storedPlayerData.get(currentPlayerCursor);
//        } while (nextPlayer.getHasLost() || currentPlayerCursor == 0);
//

        nextPlayer.getConnection().sendTCP(new YourTurnEvent());
    }

    public void endRound() {

        roundStarted = false;

        checkWinners();

        if (finalRound) {
            for (PlayerServerData player : storedPlayerData) {
                server.sendToTCP(player.getConnection().getID(), new EndOfGameEvent(player.playerName, player.getTokens()));

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                player.getConnection().close();


            }

            resetGame();

        }

        for (PlayerServerData storedPlayerData : storedPlayerData) {

            storedPlayerData.setStand(false);
            storedPlayerData.setBet(0);
            storedPlayerData.getPlayerHand().clearHand();


        }


        sendGameUpdateToPlayers();

        resetRound();

    }

    public boolean allPlayersHaveStand() {

        System.out.println(storedPlayerData.size());
        int i = 0;

        for (PlayerServerData storedPlayerData : storedPlayerData) {
            if (!storedPlayerData.getStand() && !storedPlayerData.getHasLost()) {
                System.out.println("not all players have stand");
                System.out.println(i++);
                return false;
            }
        }
        System.out.println("all players have stand");
        return true;

    }

    public void dealerPlay() {

        while (dealer.getHand().getHandValue() < 17) {
            dealer.getHand().addCard(shoe.getCardFromShoe());
            sendGameUpdateToPlayers();
        }


        endRound();

    }


    public static void main(String[] args) throws IOException {
        new ServerLauncher();
    }

    public void checkWinners() {

        HashMap<String, Integer> currWinnings = new HashMap<>();


        for (PlayerServerData storedPlayerData : storedPlayerData) {
            if (storedPlayerData.getPlayerHand().getHandValue() <= MAX_HAND_VALUE) {
                if (storedPlayerData.getPlayerHand().getHandValue() > dealer.getHand().getHandValue()) {
                    storedPlayerData.setTokens(storedPlayerData.getTokens() + storedPlayerData.getBet() * 2);
                    currWinnings.put(storedPlayerData.playerName, storedPlayerData.getBet() * 2);
                } else if (storedPlayerData.getPlayerHand().getHandValue() == dealer.getHand().getHandValue()) {
                    storedPlayerData.setTokens(storedPlayerData.getTokens() + storedPlayerData.getBet());
                    currWinnings.put(storedPlayerData.playerName, -storedPlayerData.getBet());
                } else {
                    storedPlayerData.setTokens(storedPlayerData.getTokens() + storedPlayerData.getBet());
                    currWinnings.put(storedPlayerData.playerName, -storedPlayerData.getBet());
                }

            } else {
                storedPlayerData.setTokens(storedPlayerData.getTokens() + storedPlayerData.getBet());
                currWinnings.put(storedPlayerData.playerName, -storedPlayerData.getBet());
            }
        }

        sendCurrentWinnings(currWinnings);

        for (PlayerServerData storedPlayerData : storedPlayerData) {
            if (storedPlayerData.getTokens() <= 0) {
                storedPlayerData.setHasLost(true);

                Connection connection = storedPlayerData.getConnection();

                EndOfGameEvent endOfGameEvent = new EndOfGameEvent(storedPlayerData.playerName, storedPlayerData.getTokens());
                server.sendToTCP(connection.getID(), endOfGameEvent);

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                connection.close();

                this.storedPlayerData.remove(storedPlayerData);


            }
        }

    }

    public void sendCurrentWinnings(HashMap<String, Integer> currWinnings) {

        CurrentRoundWinnings currentRoundWinnings = new CurrentRoundWinnings();

        currentRoundWinnings.setCurrentWinnings(currWinnings);

        server.sendToAllTCP(currentRoundWinnings);

    }


    public PlayerServerData getPlayerByConnection(Connection connection) {
        for (PlayerServerData serverPlayer : storedPlayerData) {
            if (serverPlayer.getConnection() == connection) {
                return serverPlayer;
            }
        }
        return null;
    }

    public PlayerServerData getCurrentTurnPlayer() {

        return storedPlayerData.get(currentPlayerCursor);

    }

    public boolean areAllBetsPlaced() {
        for (PlayerServerData storedPlayerData : storedPlayerData) {
            if (storedPlayerData.getBet() == 0) {
                return false;
            }
        }
        return true;
    }

    boolean isNicknameTaken(String nickname) {
        for (PlayerServerData storedPlayerData : storedPlayerData) {
            if (storedPlayerData.playerName.equals(nickname)) {
                return true;
            }
        }
        return false;
    }

}
