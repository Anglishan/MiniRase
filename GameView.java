package com.example.minirace;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class GameView extends SurfaceView implements Runnable {
    private Thread gameThread;
    private volatile boolean isPlaying;
    private Paint paint;
    private SurfaceHolder surfaceHolder;
    private int screenX, screenY;
    private float carX, carY;
    private int lane; // 0: left, 1: right
    private float laneX[] = new float[2];
    private float carWidth = 100, carHeight = 200;
    private List<Obstacle> obstacles;
    private long obstacleTimer;
    private Random random;
    private boolean isGameOver;

    public GameView(Context context) {
        super(context);
        surfaceHolder = getHolder();
        paint = new Paint();
        random = new Random();
        obstacles = new ArrayList<>();
    }

    @Override
    public void run() {
        // Initialize dimensions
        if (screenX == 0) {
            screenX = getWidth();
            screenY = getHeight();
            laneX[0] = screenX / 4f - carWidth / 2f;
            laneX[1] = 3 * screenX / 4f - carWidth / 2f;
            lane = 0;
            carX = laneX[lane];
            carY = screenY - carHeight - 20;
            obstacleTimer = System.currentTimeMillis();
        }

        while (isPlaying) {
            update();
            draw();
            control();
        }
    }

    private void update() {
        if (isGameOver) return;

        // Move obstacles down
        Iterator<Obstacle> iter = obstacles.iterator();
        while (iter.hasNext()) {
            Obstacle obs = iter.next();
            obs.y += obs.speed;
            if (obs.y > screenY) {
                iter.remove();
            } else {
                // Check collision
                if (obs.lane == lane) {
                    if (obs.y + obs.height >= carY && obs.y <= carY + carHeight) {
                        isGameOver = true;
                    }
                }
            }
        }

        // Spawn new obstacles
        long now = System.currentTimeMillis();
        if (now - obstacleTimer > 1500) {
            obstacleTimer = now;
            Obstacle obs = new Obstacle();
            obs.lane = random.nextInt(2);
            obs.x = laneX[obs.lane];
            obs.y = -200;
            obs.width = carWidth;
            obs.height = 200;
            obs.speed = 10 + random.nextFloat() * 10;
            obstacles.add(obs);
        }
    }

    private void draw() {
        if (surfaceHolder.getSurface().isValid()) {
            Canvas canvas = surfaceHolder.lockCanvas();
            // Draw background
            canvas.drawColor(Color.DKGRAY);
            paint.setColor(Color.WHITE);
            paint.setStrokeWidth(10);

            // Draw lane divider
            canvas.drawLine(screenX / 2f, 0, screenX / 2f, screenY, paint);

            // Draw car
            paint.setColor(Color.BLUE);
            canvas.drawRect(carX, carY, carX + carWidth, carY + carHeight, paint);

            // Draw obstacles
            paint.setColor(Color.RED);
            for (Obstacle obs : obstacles) {
                canvas.drawRect(obs.x, obs.y, obs.x + obs.width, obs.y + obs.height, paint);
            }

            // If game over, draw text
            if (isGameOver) {
                paint.setColor(Color.YELLOW);
                paint.setTextSize(100);
                canvas.drawText("Game Over", screenX / 4f, screenY / 2f, paint);
            }

            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }

    private void control() {
        try {
            Thread.sleep(17);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void pause() {
        isPlaying = false;
        try {
            if (gameThread != null)
                gameThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void resume() {
        isPlaying = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isGameOver && event.getAction() == MotionEvent.ACTION_DOWN) {
            // Restart game
            isGameOver = false;
            obstacles.clear();
            lane = 0;
            carX = laneX[lane];
            obstacleTimer = System.currentTimeMillis();
            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            // Switch lane based on touch position
            if (x < screenX / 2f) {
                lane = 0;
            } else {
                lane = 1;
            }
            carX = laneX[lane];
        }
        return true;
    }

    // Inner class for obstacles
    private class Obstacle {
        float x, y, width, height, speed;
        int lane;
    }
}
