package com.duszki.blackjack.server.player;

<<<<<<< Updated upstream:server/src/main/java/com/duszki/blackjack/server/Player/PlayerServerData.java
import com.duszki.blackjack.server.Card.Hand;
=======
import com.duszki.blackjack.server.card.Hand;
import com.duszki.blackjack.server.Request;
import com.duszki.blackjack.server.Response;
>>>>>>> Stashed changes:server/src/main/java/com/duszki/blackjack/server/Player/PlayerServerDataParser.java

public class PlayerServerData {
    private Hand playerHand;
    private int coins;

    public Hand getPlayerHand() {
        return playerHand;
    }

    public void setPlayerHand(Hand playerHand) {
        this.playerHand = playerHand;
    }

    public int getCoins() {
        return coins;
    }

    public void setCoins(int coins) {
        this.coins = coins;
    }





}
