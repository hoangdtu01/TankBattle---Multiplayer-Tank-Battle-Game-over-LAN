package com.game.ui.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.game.RingDuelGame;

public class HowToPlayScreen implements Screen {

    private final RingDuelGame game;
    private final Screen backScreen;
    private Stage stage;
    private Skin skin;
    private Texture backgroundTexture;
    private SpriteBatch batch;

    public HowToPlayScreen(RingDuelGame game, Screen backScreen) {
        this.game = game;
        this.backScreen = backScreen;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        backgroundTexture = new Texture(Gdx.files.internal("asset/mainmenu.png"));
        backgroundTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        skin = new Skin(Gdx.files.internal("asset/uiskin.json"));

        Table table = new Table();
        table.setFillParent(true);
        table.pad(40);
        stage.addActor(table);

        Label titleLabel = new Label("HOW TO PLAY", skin);
        titleLabel.getColor().a = 0.7f;
        table.add(titleLabel).padBottom(30).row();

        Label infoLabel = new Label(
                "Move: W A S D\n" +
                "Shoot: J\n" +
                "Dash: K\n" +
                "Shield: L\n" +
                "Reload: R\n\n" +
                "Goal: Knock opponents out of the arena!",
                skin
        );
        infoLabel.getColor().a = 0.7f;
        table.add(infoLabel).padBottom(30).row();

        TextButton backBtn = new TextButton("BACK", skin);
        backBtn.getColor().a = 0.7f;
        table.add(backBtn).width(200);

        backBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(backScreen);
            }
        });
    }

    @Override public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        // Draw background
        if (backgroundTexture != null && batch != null) {
            batch.begin();
            float width = Gdx.graphics.getWidth();
            float height = Gdx.graphics.getHeight();
            batch.draw(backgroundTexture, 0, 0, width, height);
            batch.end();
        }
        
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int w, int h) { stage.getViewport().update(w, h, true); }
    @Override public void dispose() { 
        if (backgroundTexture != null) backgroundTexture.dispose();
        if (batch != null) batch.dispose();
        stage.dispose(); 
        skin.dispose(); 
    }
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
}

