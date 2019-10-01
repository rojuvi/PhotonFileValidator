package photon.file.parts;

import java.util.StringJoiner;

/**
 * by donkikote on 2019-09-30.
 */
public class PhotonMultiLayerIsland {
    private int start;
    private int end;
    private PhotonRect rect;

    public PhotonMultiLayerIsland(int start, PhotonRect rect) {
        this.start = start;
        this.end = start;
        this.rect = rect;
    }

    public PhotonMultiLayerIsland(int start, int end, PhotonRect rect) {
        this.start = start;
        this.end = end;
        this.rect = rect;
    }

    public PhotonMultiLayerIsland(int start, int x1, int y1, int x2, int y2) {
        this.start = start;
        this.end = start;
        rect = new PhotonRect(x1, y1, x2, y2);
    }

    public PhotonMultiLayerIsland(int start, int end, int x1, int y1, int x2, int y2) {
        this.start = start;
        this.end = end;
        rect = new PhotonRect(x1, y1, x2, y2);
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public PhotonRect getRect() {
        return rect;
    }

    public boolean sharesLayersWith(PhotonMultiLayerIsland photonMultiLayerIsland) {
        return (this.getStart() >= photonMultiLayerIsland.getStart() && this.getStart() <= photonMultiLayerIsland.getEnd())
                || (photonMultiLayerIsland.getStart() >= this.getStart() && photonMultiLayerIsland.getStart() <= this.getEnd());
    }

    public boolean collidesWith(PhotonMultiLayerIsland photonMultiLayerIsland) {
        return this.sharesLayersWith(photonMultiLayerIsland)
                && this.getRect().collidesWith(photonMultiLayerIsland.getRect());
    }

    public boolean inContactWith(PhotonMultiLayerIsland photonMultiLayerIsland) {
        return this.sharesLayersWith(photonMultiLayerIsland)
                && this.getRect().inContactWith(photonMultiLayerIsland.getRect());
    }

    public void merge(PhotonMultiLayerIsland photonMultiLayerIsland) {
        this.start = Math.min(this.start, photonMultiLayerIsland.getStart());
        this.end = Math.max(this.end, photonMultiLayerIsland.getEnd());
        this.rect.merge(photonMultiLayerIsland.getRect());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", PhotonMultiLayerIsland.class.getSimpleName() + "[", "]")
                .add("start=" + start)
                .add("end=" + end)
                .add("rect=" + rect)
                .toString();
    }
}
