import java.util.ArrayList;
import java.util.List;
import java.awt.*;

public class main {
    public static void main(String[] args) {
        Point a=new Point();
        Point b=new Point();
        a.x=1;
        a.y=2;
        b.x=3;
        b.y=4;
        List<Point> list=new ArrayList<>();
        list.add((Point) a);
        list.add((Point) a);
        System.out.println(list);
    }
}
