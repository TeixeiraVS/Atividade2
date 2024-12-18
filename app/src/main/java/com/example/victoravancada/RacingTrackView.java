package com.example.victoravancada;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

public class RacingTrackView extends View {

    private List<Car> cars;
    private Bitmap trackBitmap;
    private Bitmap cachedTrackBitmap; // Bitmap para cache da pista

    public RacingTrackView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setCars(List<Car> cars) {
        this.cars = cars;
        invalidate();
    }

    public List<Car> getCars() {
        return cars;
    }

    public void setTrackBitmap(Bitmap trackBitmap) {
        this.trackBitmap = trackBitmap;
        this.cachedTrackBitmap = null; // Recria o cache se a pista for alterada
        invalidate();
        // Calcula a distância da pista
    }

    /**
     * Cria o bitmap de cache da pista.
     */
    private void createCachedTrack() {
        if (trackBitmap != null) {
            cachedTrackBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            Canvas cachedCanvas = new Canvas(cachedTrackBitmap);
            cachedCanvas.drawBitmap(trackBitmap, 0, 0, null);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Desenha a pista do cache ou recria se necessário
        if (cachedTrackBitmap == null) {
            createCachedTrack();
        }
        if (cachedTrackBitmap != null) {
            canvas.drawBitmap(cachedTrackBitmap, 0, 0, null);
        }

        // Desenha os carros
        if (cars != null && !cars.isEmpty()) {
            for (Car car : cars) {
                Bitmap tintedCarImage = car.getTintedCarImage();
                if (tintedCarImage != null) {
                    canvas.save();
                    // Rotaciona o carro em torno de seu centro
                    canvas.rotate(car.getDirectionAngle(), car.getX(), car.getY());
                    // Desenha o carro com base em sua posição e ângulo
                    canvas.drawBitmap(tintedCarImage, car.getX() - tintedCarImage.getWidth() / 2f,
                            car.getY() - tintedCarImage.getHeight() / 2f, null);
                    canvas.restore();
                }
            }
        }
    }
}
