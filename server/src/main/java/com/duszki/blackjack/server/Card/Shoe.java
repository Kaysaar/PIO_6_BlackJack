package com.duszki.blackjack.server.Card;

import com.duszki.blackjack.shared.models.Card;

import java.util.ArrayList;

public class Shoe {

    static final int DECK_QUANTITY = 8;
    private ArrayList<Deck> deckList = new ArrayList<>();

    public Shoe(){

        for(int i = 0; i < DECK_QUANTITY; i++){
            Deck deck = new Deck();
            deckList.add(deck);
        }
    }
    public void lastDeckRound(){
        Deck deck = new Deck();
        deckList.add(deck);
    }

    Deck getFirstAvailableDeck(){
        if(deckList.isEmpty()){
            return null;
        }
        return deckList.get(0);

    }
    void removeEmptyDeck(Deck currDeck){
        if(currDeck==null) return;
        if(currDeck.getCardsInDeck().isEmpty()){
            deckList.remove(currDeck);
        }
    }
    public Deck replaceDeckIfNeeded(Deck currDeck){
        removeEmptyDeck(currDeck);
        return getFirstAvailableDeck();
    }

    public Card getCardFromShoe(){
        Deck curentDeck = getFirstAvailableDeck();
        if(curentDeck.getCardsInDeck().isEmpty()){
            curentDeck = replaceDeckIfNeeded(getFirstAvailableDeck());
        }
        if(curentDeck==null){
            return null;
        }
        return curentDeck.removeCard();
    }

}
