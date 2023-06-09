package com.duszki.blackjack;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import com.esotericsoftware.kryonet.*;

/**
 * {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms.
 */
public class Blackjack extends Game {
    private Homepage homepage;
    private Board board;
    private Game game;

    @Override
    public void create() {
        game = this;
        homepage = new Homepage(this);
//        board = new Board(this);
        setScreen(homepage);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
    }

    @Override
    public void render() {
        super.render();
    }

    @Override
    public void dispose() {
        super.dispose();
        homepage.dispose();
    }

}
