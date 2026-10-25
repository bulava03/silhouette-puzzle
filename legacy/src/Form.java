import javax.swing.*;
import java.awt.*;

public class Form extends JFrame {

    public Form() {
        initComponents();
    }

    private void initComponents() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Form1");
        setBounds(-552, 153, 895, 520);
        setLocationRelativeTo(null);
        getContentPane().setLayout(null);

        Shape shape1 = new Shape();
        shape1.setBounds(440, 24, 566, 112);
        shape1.setBackground(Color.BLUE);
        getContentPane().add(shape1);

        Shape shape2 = new Shape();
        shape2.setBounds(120, 8, 97, 64);
        shape2.setBackground(Color.YELLOW);
        getContentPane().add(shape2);

        Shape shape3 = new Shape();
        shape3.setBounds(304, 176, 568, 283);
        shape3.setBackground(Color.LIGHT_GRAY);
        getContentPane().add(shape3);

        Shape shape4 = new Shape();
        shape4.setBounds(105, 40, 448, 120);
        shape4.setBackground(Color.GREEN);
        getContentPane().add(shape4);

        Shape shape5 = new Shape();
        shape5.setBounds(488, 167, 65, 121);
        shape5.setBackground(Color.GREEN);
        getContentPane().add(shape5);

        Shape shape6 = new Shape();
        shape6.setBounds(232, 16, 40, 66);
        shape6.setBackground(Color.BLUE);
        getContentPane().add(shape6);

        Shape shape7 = new Shape();
        shape7.setBounds(272, 8, 81, 49);
        shape7.setBackground(Color.BLUE);
        getContentPane().add(shape7);

        Shape shape8 = new Shape();
        shape8.setBounds(40, 424, 81, 49);
        shape8.setBackground(Color.BLUE);
        getContentPane().add(shape8);

        Shape shape9 = new Shape();
        shape9.setBounds(272, 424, 81, 49);
        shape9.setBackground(Color.BLUE);
        getContentPane().add(shape9);

        Shape shape10 = new Shape();
        shape10.setBounds(672, 256, 40, 88);
        shape10.setBackground(Color.GREEN);
        getContentPane().add(shape10);

        Shape shape11 = new Shape();
        shape11.setBounds(616, 200, 40, 36);
        shape11.setBackground(Color.BLUE);
        getContentPane().add(shape11);

        Shape shape12 = new Shape();
        shape12.setBounds(568, 312, 41, 80);
        shape12.setBackground(Color.GREEN);
        getContentPane().add(shape12);

        Shape shape13 = new Shape();
        shape13.setBounds(192, 416, 40, 34);
        shape13.setBackground(Color.BLUE);
        getContentPane().add(shape13);

        Shape shape14 = new Shape();
        shape14.setBounds(48, 104, 64, 57);
        shape14.setBackground(Color.BLACK);
        getContentPane().add(shape14);

        Shape shape15 = new Shape();
        shape15.setBounds(368, 176, 64, 57);
        shape15.setBackground(Color.BLACK);
        getContentPane().add(shape15);

        Shape shape16 = new Shape();
        shape16.setBounds(464, 264, 64, 57);
        shape16.setBackground(Color.BLACK);
        getContentPane().add(shape16);

        Shape shape17 = new Shape();
        shape17.setBounds(8, 360, 570, 41);
        shape17.setBackground(Color.LIGHT_GRAY);
        getContentPane().add(shape17);

        Shape shape18 = new Shape();
        shape18.setBounds(48, 24, 17, 49);
        shape18.setBackground(Color.GRAY);
        getContentPane().add(shape18);

        Shape shape19 = new Shape();
        shape19.setBounds(392, 32, 17, 49);
        shape19.setBackground(Color.GRAY);
        getContentPane().add(shape19);

        Shape shape20 = new Shape();
        shape20.setBounds(800, 144, 17, 49);
        shape20.setBackground(Color.GRAY);
        getContentPane().add(shape20);

        Shape shape21 = new Shape();
        shape21.setBounds(528, 8, 17, 49);
        shape21.setBackground(Color.GRAY);
        getContentPane().add(shape21);

        Shape shape22 = new Shape();
        shape22.setBounds(512, 136, 17, 49);
        shape22.setBackground(Color.GRAY);
        getContentPane().add(shape22);

        Shape shape23 = new Shape();
        shape23.setBounds(560, 144, 17, 49);
        shape23.setBackground(Color.GRAY);
        getContentPane().add(shape23);

        Shape shape24 = new Shape();
        shape24.setBounds(616, 160, 17, 49);
        shape24.setBackground(Color.GRAY);
        getContentPane().add(shape24);

        Shape shape25 = new Shape();
        shape25.setBounds(688, 136, 17, 49);
        shape25.setBackground(Color.GRAY);
        getContentPane().add(shape25);

        ShapeMouseListener listener = new ShapeMouseListener();
        shape1.addMouseListener(listener);
        shape2.addMouseListener(listener);
        shape3.addMouseListener(listener);
        shape4.addMouseListener(listener);
        shape5.addMouseListener(listener);
        shape6.addMouseListener(listener);
        shape7.addMouseListener(listener);
        shape8.addMouseListener(listener);
        shape9.addMouseListener(listener);
        shape10.addMouseListener(listener);
        shape11.addMouseListener(listener);
        shape12.addMouseListener(listener);
        shape13.addMouseListener(listener);
        shape14.addMouseListener(listener);
        shape15.addMouseListener(listener);
        shape16.addMouseListener(listener);
        shape17.addMouseListener(listener);
        shape18.addMouseListener(listener);
        shape19.addMouseListener(listener);
        shape20.addMouseListener(listener);
        shape21.addMouseListener(listener);
        shape22.addMouseListener(listener);
        shape23.addMouseListener(listener);
        shape24.addMouseListener(listener);
        shape25.addMouseListener(listener);

        shape1.addMouseMotionListener(listener);
        shape2.addMouseMotionListener(listener);
        shape3.addMouseMotionListener(listener);
        shape4.addMouseMotionListener(listener);
        shape5.addMouseMotionListener(listener);
        shape6.addMouseMotionListener(listener);
        shape7.addMouseMotionListener(listener);
        shape8.addMouseMotionListener(listener);
        shape9.addMouseMotionListener(listener);
        shape10.addMouseMotionListener(listener);
        shape11.addMouseMotionListener(listener);
        shape12.addMouseMotionListener(listener);
        shape13.addMouseMotionListener(listener);
        shape14.addMouseMotionListener(listener);
        shape15.addMouseMotionListener(listener);
        shape16.addMouseMotionListener(listener);
        shape17.addMouseMotionListener(listener);
        shape18.addMouseMotionListener(listener);
        shape19.addMouseMotionListener(listener);
        shape20.addMouseMotionListener(listener);
        shape21.addMouseMotionListener(listener);
        shape22.addMouseMotionListener(listener);
        shape23.addMouseMotionListener(listener);
        shape24.addMouseMotionListener(listener);
        shape25.addMouseMotionListener(listener);
    }

}
