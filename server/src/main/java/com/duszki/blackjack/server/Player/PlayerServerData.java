package com.duszki.blackjack.server.Player;

import com.duszki.blackjack.shared.models.Hand;
import com.esotericsoftware.kryonet.Connection;

public class PlayerServerData {

    private Connection connection;

    private Hand playerHand;
    private boolean agreedToPlay = false;
    private int tokens;
    public String playerName;
    private boolean hasLost;
    private boolean stand;

    private boolean betPlaced = false;

    private int bet;

    public PlayerServerData(Connection connection, String playerName) {
        this.connection = connection;
        this.playerName = playerName;
        this.stand = false;
        this.hasLost = false;

    }

    public PlayerServerData() {

    }

    public Connection getConnection() {
        return this.connection;
    }

    public Hand getPlayerHand() {
        return playerHand;
    }

    public void setPlayerHand(Hand playerHand) {
        this.playerHand = playerHand;
    }

    public int getTokens() {
        return tokens;
    }

    public void setTokens(int tokens) {
        this.tokens = tokens;
    }

    public void setStand(boolean stand) {
        this.stand = stand;
    }

    public boolean getStand() {
        return this.stand;
    }

    public void setHasLostRound(boolean hasLost) {
        this.hasLost = hasLost;
    }

    public boolean getHasLost() {
        return this.hasLost;
    }

    public int getBet() {
        return bet;
    }

    public void setBet(int i) {
        this.bet = i;
    }

    public boolean getBetPlaced() {
        return betPlaced;
    }

    public void setBetPlaced(boolean betPlaced) {
        this.betPlaced = betPlaced;
    }


    public void setHasLost(boolean hasLost) {
        this.hasLost = hasLost;

    }
}
