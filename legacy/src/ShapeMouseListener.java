import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ShapeMouseListener extends MouseAdapter {

    static int x1, y1;
    static boolean flag = false;

    @Override
    public void mousePressed(MouseEvent e) {
        flag = true;
        x1 = e.getX();
        y1 = e.getY();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        flag = false;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (ShapeMouseListener.flag) {
            Component source = (Component) e.getSource();
            source.setLocation(source.getX() - ShapeMouseListener.x1 + e.getX(), source.getY() - ShapeMouseListener.y1 + e.getY());
        }
    }

}
