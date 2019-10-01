package photon.file.parts;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * by donkikote on 2019-09-30.
 */
public class PhotonMultiLayerIsland implements Comparable {
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

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PhotonMultiLayerIsland)) return false;
        int compare = this.compareTo(o);
        return compare == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getStart(), getEnd(), getRect());
    }

    @Override
    public int compareTo(Object o) {
        PhotonMultiLayerIsland other = (PhotonMultiLayerIsland) o;

        int i = Integer.compare(start, other.getStart());
        if (i != 0) return i;

        i = Integer.compare(end, other.getEnd());
        if (i != 0) return i;
        int i1 = rect.compareTo(other.getRect());
        return i1;
    }
}
