package com.duszki.blackjack;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.awt.*;
import java.util.ArrayList;

import com.duszki.blackjack.shared.data.*;
import com.duszki.blackjack.shared.events.*;
import com.duszki.blackjack.shared.models.Card;
import com.duszki.blackjack.shared.player.PlayerTransferData;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.minlog.Log;

public class Board implements Screen {
    private static final boolean DEBUG = true;
    private Skin skin;
    private Stage stage;
    private Game game;

    private float width;
    private float height;
    private float aspectRatio;
    private SpriteBatch batch;
    private Texture backgroundTexture;
    private OrthographicCamera camera;
    private FitViewport viewport;

    ImageButton buttonHit;
    ImageButton buttonStand;
    ImageButton buttonDouble;

    private ArrayList<UnrevealedCard> Hand;

    private DataToTransfer currentGameState;

    private Client client;

    private int cardsInHand;

    private int cardsInDealer;

    private ArrayList<UnrevealedCard> Dealer;

    public Board(Game game) {

        if (DEBUG) {
//            Log.ERROR();
//            Log.WARN();
//            Log.INFO();
            Log.DEBUG();
//            Log.TRACE();
        }

        this.client = NetworkManager.getClient();

        this.game = game;
        aspectRatio = (float) Gdx.graphics.getWidth() / (float) Gdx.graphics.getHeight();
        height = 1000;
        width = height * aspectRatio;

        batch = new SpriteBatch();

        backgroundTexture = new Texture("Board1728x1117.png");

        camera = new OrthographicCamera();

        camera.position.set(width / 2f, height / 2f, 0);
        viewport = new FitViewport(width, height, camera);

        viewport.apply();

        stage = new Stage(viewport);
        skin = new Skin(Gdx.files.internal("Board/skin.json"));
        MyInputProcessor myInputProcessor = new MyInputProcessor();
        InputMultiplexer multiplexer = new InputMultiplexer(myInputProcessor, stage);
        Gdx.input.setInputProcessor(multiplexer);

        Bet bet = new Bet(this);
//        stage.addActor(bet.getTable());

        Balance balance = new Balance(this);
        stage.addActor(balance.getTable());


        Hand = new ArrayList<>();
        Dealer = new ArrayList<>();

        cardsInHand = 0;

        cardsInDealer = 0;

        buttonHit = new ImageButton(skin, "Hit");
        buttonHit.setPosition(width - width / 5, 300);
        stage.addActor(buttonHit);
        buttonHit.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {

                HitEvent hitEvent = new HitEvent();
                client.sendTCP(hitEvent);
                buttonDouble.setVisible(false);

            }
        });

        buttonStand = new ImageButton(skin, "Stand");
        buttonStand.setPosition(width - width / 5, 200);

        stage.addActor(buttonStand);

        buttonStand.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                StandEvent standEvent = new StandEvent();
                client.sendTCP(standEvent);

            }
        });


        buttonDouble = new ImageButton(skin, "Double");
        buttonDouble.setPosition(width - width / 5, 100);

        stage.addActor(buttonDouble);

        buttonDouble.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                DoubleDownEvent doubleEvent = new DoubleDownEvent();
                client.sendTCP(doubleEvent);
                buttonDouble.setVisible(false);

            }
        });

        client.addListener(new Listener() {
            public void received(Connection connection, Object object) {
                if (object instanceof RoundStartEvent) {
                    stage.addActor(bet.getTable());

                }
            }
        });

        bet.getButton().addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {

                // send bet to server
                PlaceBetEvent placeBetEvent = new PlaceBetEvent();
                String betValue = bet.getBet();

                int betInt;

                try {
                    betInt = Integer.parseInt(betValue);
                } catch (NumberFormatException e) {
                    return;
                }

                // remove bet actor from stage
                stage.getActors().removeValue(bet.getTable(), true);

                placeBetEvent.setBet(betInt);

                client.sendTCP(placeBetEvent);


            }
        });

        client.addListener(new Listener() {
            public void received(Connection connection, Object object) {
                if (object instanceof PlayerTransferData) {
                    PlayerTransferData request = (PlayerTransferData) object;

                    balance.setBalance(Integer.toString(request.getBalance()));

                    ArrayList<Card> cards = request.getCards();
                    if (cards.size() == 2 && cardsInHand == 0) {
                        cardsInHand = cards.size();
                        for (Card card : cards) {
                            System.out.println(card.toString());
                            Gdx.app.postRunnable(new Runnable() {
                                @Override
                                public void run() {
                                    addCardBoard(card.toString());
                                }
                            });

                        }
                    } else {
                        for (int i = cardsInHand; i < cards.size(); i++) {
                            System.out.println(cards.get(i).toString());
                            Card card = cards.get(i);
                            Gdx.app.postRunnable(new Runnable() {
                                @Override
                                public void run() {
                                    addCardBoard(card.toString());
                                }
                            });

                            cardsInHand++;

                        }

                    }
                }


            }

        });

        client.addListener(new Listener() {
            public void received(Connection connection, Object object) {
                if (object instanceof DataToTransfer) {
                    DataToTransfer request = (DataToTransfer) object;


                    ArrayList<Card> cards = request.dealerHand.getCardsInHand();
                    if (cards.size() == 2 && cardsInDealer == 0) {
                        cardsInDealer = cards.size();
                        for (Card card : cards) {
                            System.out.println(card.toString());
                            Gdx.app.postRunnable(new Runnable() {
                                @Override
                                public void run() {
                                    addCardforDealer(card.toString());
                                }
                            });

                        }
                    } else {
                        for (int i = cardsInDealer; i < cards.size(); i++) {
                            System.out.println(cards.get(i).toString());
                            Card card = cards.get(i);
                            Gdx.app.postRunnable(new Runnable() {
                                @Override
                                public void run() {
                                    addCardforDealer(card.toString());
                                }
                            });

                            cardsInDealer++;

                        }

                    }
                }


            }


        });


    }


    void addCardBoard(String card) {
        UnrevealedCard unrevealedCard = new UnrevealedCard(card);
        unrevealedCard.setAction(Hand.size());
        stage.addActor(unrevealedCard.getImage());
        Hand.add(unrevealedCard);

    }

    void addCardforDealer(String card) {
//        UnrevealedCard unrevealedCard;
//        if(Dealer.size() == 0) {
//            unrevealedCard = new UnrevealedCard("back");
//        }else {
//            unrevealedCard = new UnrevealedCard("10_of_clubs");
//        }

        UnrevealedCard unrevealedCard = new UnrevealedCard(card);

        unrevealedCard.setDealerAction(Dealer.size());
        stage.addActor(unrevealedCard.getImage());
        Dealer.add(unrevealedCard);
    }

    void removeCards() {
        for (UnrevealedCard unrevealedCard : Hand) {
            unrevealedCard.getImage().addAction(Actions.removeActor());
        }
        Hand.clear();
        for (UnrevealedCard unrevealedCard : Dealer) {
            unrevealedCard.getImage().addAction(Actions.removeActor());
        }
        Dealer.clear();
    }


    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 0);
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.draw(backgroundTexture, 0, 0, width, height);
        batch.end();
        stage.act();
        stage.draw();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    public void dispose() {
        stage.dispose();
        skin.dispose();
    }
}
