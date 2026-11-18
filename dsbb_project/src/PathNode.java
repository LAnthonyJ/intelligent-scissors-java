import java.awt.*;

public class PathNode {
    final Point point;
    int stableCount;

    PathNode(Point p) {
        this.point = new Point(p);
        this.stableCount = 0;
    }

    boolean samePosition(Point other) {
        return point.x == other.x && point.y == other.y;
    }
}
