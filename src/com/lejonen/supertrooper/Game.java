package com.lejonen.supertrooper;

import com.googlecode.lanterna.TerminalFacade;
import com.googlecode.lanterna.input.Key;
import com.googlecode.lanterna.terminal.Terminal;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class Game {

    //WIDTH and HEIGHT final
    public static final int WIDTH = 100;
    public static final int HEIGHT = 30;
    public static final int CEILING = 2;
    public static int fps = 0;
    public static int level = 1;
    public static int levelDrawTimer = 0;
    public static int prefUPS = 60;
    public static int prefFPS = 30;

    private Player player;
    private Terminal terminal;

    List<Creature> creatures;
    List<Shot> shots;

    public Game() {

        initializeTerminal();
        initializePlayer();
        initializeCreatures();
        initializeShots();
    }

    public void startGame() {

        gameLoop();
        System.out.println("Game over");
    }

    public void update() {

        handleInput();
        updateCreatures();
        updateShots();
        checkCollisions();
        checkPlayerAlive();
        updateLevel();
    }

    public void render() {

        terminal.clearScreen();

        Draw.drawHeader(terminal, player);
        Draw.drawPlayer(terminal, player);

        for (Creature creature : creatures) {
            Draw.drawCreature(terminal, creature);
        }

        for (Shot shot : shots) {
            Draw.drawShot(terminal, shot);
        }
    }

    private void initializeTerminal() {
        terminal = TerminalFacade.createTerminal(System.in, System.out, Charset.forName("UTF8"));
        terminal.enterPrivateMode();
        terminal.setCursorVisible(false);
    }

    private void initializePlayer() {
        player = new Player(WIDTH / 2, HEIGHT, 2);
    }

    private void initializeShots() {
        shots = new ArrayList<>();
    }

    private void initializeCreatures() {
        creatures = new ArrayList<>();
    }

    private void gameLoop() {
        boolean RENDER_TIME = true;
        long initialTime = System.nanoTime();
        final double timeU = 1000000000 / prefUPS; //Updates per second
        final double timeF = 1000000000 / prefFPS; //FPS;
        double deltaU = 0, deltaF = 0;
        int frames = 0, ticks = 0;
        long timer = System.currentTimeMillis();

        //TODO: Change condition to life greater than zero.
        while (player.isAlive) {

            long currentTime = System.nanoTime();
            deltaU += (currentTime - initialTime) / timeU;
            deltaF += (currentTime - initialTime) / timeF;
            initialTime = currentTime;

            if (deltaU >= 1) {
                update();
                ticks++;
                deltaU--;
            }

            if (deltaF >= 1) {
                render();
                if (levelDrawTimer>0) {
                    Draw.drawLevelUp(terminal);
                    levelDrawTimer--;
                }
                frames++;
                deltaF--;
            }

            if (System.currentTimeMillis() - timer > 1000) {
                if (RENDER_TIME) {
                    System.out.println(String.format("UPS: %s, FPS: %s", ticks, frames));
                    fps = frames;
                    System.out.println("SCORE: " + player.score + "     LIFE: " + player.life);
                }
                frames = 0;
                ticks = 0;
                timer += 1000;
            }
        }
        gameOver();
    }

    private void gameOver() {

        Draw.drawGameOver(terminal);
        HighScore.checkScore(terminal, player);
        Draw.drawHighScore(terminal, HighScore.highScores);

        //TODO: Load highscore table from file.
        //TODO: Check user score against highscore and add score to highscore if applicable.
        //TODO: Write highscore file.


    }

    public void handleInput() {

        Key key = terminal.readInput();

        if (key == null)
            return;
        switch (key.getCharacter()) {
            case 'L':
                player.moveLeft();
                break;
            case 'R':
                player.moveRight();
                break;
            case ' ':
                player.shoot(shots);
                break;
            default:
                return;
        }
    }

    public void updateCreatures() {

        //Extracted creature spawn method.
        checkForNewCreatures();

        for (Creature creature : creatures) {
            Creature.moveCreature(creature);
        }
    }

    //TODO: Finns bättre sätt att göra detta, och vi är inte hundra procent överens om vilken vi väljer.
    private void checkForNewCreatures() {
        if (Math.random() < (1.00 / (120/Game.level))) {
            if (Math.random() > 0.95) {
                creatures.add(new ExtraLife(Math.random() * WIDTH, CEILING));
            } else if (Math.random() > 0.95) {
                creatures.add(new PowerUp(Math.random() * WIDTH, CEILING));
            } else if (Math.random() > 0.80) {
                creatures.add(new FastEnemy(Math.random() * WIDTH, CEILING));
            } else {
                creatures.add(new SlowEnemy(Math.random() * WIDTH, CEILING));
            }
        }
    }

    public void updateShots() {

        for (Shot shot : shots) {
            Shot.moveShot(shot);
        }
    }

    private void checkCollisions() {

        //TODO: Maybe shot and creature should both be part of a superclass Entity to streamline the code and remove duplication.

        //Remove shots that are outside screen.
        if (shots.size() > 0) {
            for (int i = shots.size() - 1; i >= 0; i--) {
                if (shots.get(i).y < CEILING || shots.get(i).y > HEIGHT)
                    shots.remove(shots.get(i));
            }
        }

        //Remove creatures that are outside screen.
        if (creatures.size() > 0) {
            for (int i = creatures.size() - 1; i >= 0; i--) {
                if (creatures.get(i).y > HEIGHT)
                    //To do: Lägg till score minskas, alt life minskar när en ENEMY når botten av skärmen.
                    creatures.remove(creatures.get(i));
            }
        }


        //Check player collision with creatures
        if (creatures.size() > 0) {
            for (int i = creatures.size() - 1; i >= 0; i--) {
                if ((Math.abs(Math.round(creatures.get(i).y) - (Math.round(player.y))) < 0.75) && ((Math.abs(Math.round(creatures.get(i).x) - Math.round(player.x)) < 1.8))) {
                    if (creatures.get(i) instanceof Enemy) {
                        --player.life;
                    } else if (creatures.get(i) instanceof ExtraLife) {
                        ++player.life;
                    } else if (creatures.get(i) instanceof PowerUp) {
                        player.addScore(10);
                    }
                    creatures.remove(creatures.get(i));
                }
            }
        }

        //Check collision between shot and creature
        if (creatures.size() > 0 && shots.size() > 0) {

            for (int i = shots.size() - 1; i >= 0; i--) {

                int shotToRemove = -1;

                for (int j = creatures.size() - 1; j >= 0; j--) {
                    if (creatures.get(j) instanceof Enemy && Math.abs(shots.get(i).y - creatures.get(j).y) < 1 &&
                            Math.abs(shots.get(i).x - creatures.get(j).x) < 1) {

                        shotToRemove = i;
                        player.score += ((Enemy) creatures.get(j)).value;
                        creatures.remove(creatures.get(j));

                    }

                }

                if (shotToRemove >= 0)
                    shots.remove(shots.get(shotToRemove));
            }
        }
    }
    private void checkPlayerAlive() {

        if (player.life<1)
            player.isAlive=false;
    }

    private void updateLevel() {
        if (player.score>player.nextLevel) {
            player.levelUp();
            levelDrawTimer = prefUPS * 2;
            Creature.levelUpCreatures(creatures);

        }
    }
}