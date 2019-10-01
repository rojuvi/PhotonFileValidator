package photon.file.parts;

import java.util.StringJoiner;

/**
 * by donkikote on 2019-09-30.
 */
public class PhotonRect {

    private int x1;
    private int y1;
    private int x2;
    private int y2;

    public PhotonRect(int x1, int y1, int x2, int y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    public int getX1() {
        return x1;
    }

    public int getY1() {
        return y1;
    }

    public int getX2() {
        return x2;
    }

    public int getY2() {
        return y2;
    }

    public boolean inContactWith(PhotonRect rect) {
        return (verticalContact(rect) && horizontalContained(rect)) ||
        (horizontalContact(rect) && verticalContained(rect));
    }

    public boolean collidesWith(PhotonRect rect) {
        return verticalContained(rect) && horizontalContained(rect);
    }

    private boolean verticalContact(PhotonRect rect) {
        return y1 == rect.getY2() + 1 || y2 == rect.getY1() - 1;
    }

    private boolean horizontalContact(PhotonRect rect) {
        return x1 == rect.getX2() + 1 || x2 == rect.getX1() - 1;
    }

    private boolean horizontalContained(PhotonRect rect) {
        return (x1 >= rect.getX1() && x1 <= rect.getX2())
                || ( rect.getX1() >= x1 && rect.getX1() <= x2 );
    }

    private boolean verticalContained(PhotonRect rect) {
        return (y1 >= rect.getY1() && y1 <= rect.getY2())
                || (rect.getY1() >= y1 && rect.getY1() <= y2);
    }

    public void merge(PhotonRect rect) {
        x1 = Math.min(x1, rect.x1);
        x2 = Math.max(x2, rect.x2);
        y1 = Math.min(y1, rect.y1);
        y2 = Math.max(y2, rect.y2);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", PhotonRect.class.getSimpleName() + "[", "]")
                .add("x1=" + x1)
                .add("y1=" + y1)
                .add("x2=" + x2)
                .add("y2=" + y2)
                .toString();
    }
}
