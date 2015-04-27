package DLB;

import DLB.Utils.Message;
import DLB.Utils.MessageType;
import DLB.Utils.StateInfo;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.io.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.GZIPOutputStream;

/**
 * Created by manshu on 4/21/15.
 */
public class DynamicBalancerUI extends Thread {
    int numMachines = 2;

    LinkedBlockingQueue<Message> messageQueue;

    double[] throttleValues;
    StateInfo[] machineStates;
    JSpinner[] jSpinners;
    JProgressBar jProgressBar;
    JProgressBar jResultBar;
    JLabel[] jLabels;
    JLabel[] jFiles;
    JLabel tempFile;
    JTable[] jTables;
    JScrollPane[] jScrollPanes;
    JFrame jFrame;
    JLabel[] jchannelInfos;
    JLabel[] jTransInfos;

    ImageIcon red_button, green_button;

    String[] columnNames = {"Property", "Value"};

    String resourcePath = "";

    private boolean isVisible = true;

    public DynamicBalancerUI(boolean visible, String path) {
        resourcePath = path + "/" + "images";
        messageQueue = new LinkedBlockingQueue<>();
        throttleValues = new double[numMachines];
        machineStates = new StateInfo[numMachines];
        if (visible) {
            changeLooks();
            initUI();
            isVisible = true;
        } else {
            isVisible = false;
        }
    }



    private void initUI() {
        jFrame = new JFrame();
        jFrame.setLayout(new FlowLayout());
        jFrame.setSize(2150, 1560);
        jFrame.setTitle("Dynamic Load Balancer");
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.setDefaultLookAndFeelDecorated(true);

        jSpinners = new JSpinner[numMachines];
        jLabels = new JLabel[numMachines];
        jFiles = new JLabel[numMachines];
        jTables = new JTable[numMachines];
        jScrollPanes = new JScrollPane[numMachines];
        jchannelInfos = new JLabel[numMachines];
        jTransInfos = new JLabel[numMachines];

//        String path = MainThread.class.getClass().getResource("DLB/res/images").getPath();
//
//        ImageIcon machine = new ImageIcon(path + "/pc-icon.png");
//        ImageIcon file = new ImageIcon(path + "/file.png");

        //String resource_path = "res/images/";//getClass().getClassLoader().getResource("/resources/images/").getPath();//"res/images/";

        ImageIcon machine = new ImageIcon(resourcePath + "/" + "pc-icon.png");
        ImageIcon file = new ImageIcon(resourcePath + "/" + "file.png");
        red_button = new ImageIcon(resourcePath + "/" + "red-icon.png");
        green_button = new ImageIcon(resourcePath + "/" + "green-icon.png");

        tempFile = new JLabel(file);

        for (int i = 0; i < numMachines; i++) {
            //spinner properties
            jSpinners[i] = new JSpinner(new SpinnerNumberModel(MainThread.throttlingValue, 0.0, 1.0, 0.1));
            jSpinners[i].setName(String.valueOf(i));
            jSpinners[i].setPreferredSize(new Dimension(100, 50));
            //((JSpinner.DefaultEditor) jSpinners[i].getEditor()).getTextField().setEditable(false);

            //label
            jLabels[i] = new JLabel(machine);
            jFiles[i] = new JLabel(file);
            jchannelInfos[i] = new JLabel("Channel Empty");
            jTransInfos[i] = new JLabel(green_button);
            jTransInfos[i].setName("green");

            //table
            jScrollPanes[i] = new JScrollPane();
            jTables[i] = new JTable(getStateData(new StateInfo(0, 0, false, false, 0)), columnNames);
            //jTables[i].setMinimumSize(new Dimension(200, 200));
            jTables[i].setRowHeight(50);
            jTables[i].setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
            jTables[i].setFont(new Font("Monaco", Font.BOLD, 24));
            jScrollPanes[i] = new JScrollPane(jTables[i],
                    JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                    //add all containers
            jScrollPanes[i].setWheelScrollingEnabled(true);
            JScrollBar bar = jScrollPanes[i].getVerticalScrollBar();
            bar.setPreferredSize(new Dimension(30, 10));

            jFrame.add(jSpinners[i]);
            jFrame.add(jLabels[i]);
            jFrame.add(jFiles[i]);
            jFrame.add(jchannelInfos[i]);
            jFrame.add(jTransInfos[i]);
            jFrame.add(jScrollPanes[i]);
            jFrame.add(Box.createRigidArea(new Dimension(900,0)));
        }
        jProgressBar = new JProgressBar();
        jProgressBar.setStringPainted(true);
        jProgressBar.setPreferredSize(new Dimension(500, 50));
        jProgressBar.setMinimum(0);
        jProgressBar.setMaximum(100);
        jFrame.add(jProgressBar);
        setProgress(0);

        jResultBar = new JProgressBar();
        jResultBar.setStringPainted(true);
        jResultBar.setPreferredSize(new Dimension(500, 50));
        jResultBar.setMinimum(0);
        jResultBar.setMaximum(100);
        jFrame.add(jResultBar);
        setResultProgress(0);

        setActionListeners();
    }

    private void changeLooks() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                //System.out.println(info.getName());
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                }
            }
            //UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            //UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
    }

    private void changeThrottleValue(int machineId, double value) {
        if (!isVisible) return;
        if (machineId == MainThread.machineId)
            MainThread.adapterThread.setThrottlingValue(value);
        else {
            Message message = new Message(MainThread.machineId, MessageType.TVALUE, value);
            MainThread.transferManagerThread.addMessage(message);
        }
        System.out.println("Machine = " + machineId + " value = " + value);
//        StateInfo stateInfo = new StateInfo(1, 2.0, 3.0);
//        setState(machineId, stateInfo);
    }

    protected void setState(int machineId, StateInfo stateInfo) {
        if (!isVisible) return;
        String[][] datavalues = getStateData(stateInfo);
        for (int i = 0; i < datavalues.length; i++) {
            jTables[machineId].setValueAt(datavalues[i][0], i, 0);
            jTables[machineId].setValueAt(datavalues[i][1], i, 1);
        }
    }

    protected void changeTransferStatus(int machineId, boolean status) {
        if (!isVisible) return;
        if (jTransInfos[machineId].getName().equals("green")) {
            if (status) {
                jTransInfos[machineId].setName("red");
                jTransInfos[machineId].setIcon(red_button);
                jchannelInfos[machineId].setText("Channel  Send");
            }
        } else {
            if (!status) {
                jTransInfos[machineId].setName("green");
                jTransInfos[machineId].setIcon(green_button);
                jchannelInfos[machineId].setText("Channel Empty");
            }
        }
    }

    protected void setResultProgress(int progress) {
        if (!isVisible) return;
        jResultBar.setValue(progress);
        if (progress <= 25) {
            jResultBar.setForeground(Color.RED);
        } else if (progress > 25 && progress < 75) {
            jResultBar.setForeground(Color.BLUE);
        } else if (progress >= 75) {
            jResultBar.setForeground(Color.GREEN);
        }
        jResultBar.setString(progress + "%");
    }

    protected void setProgress(int progress) {
        //if (progress < 0) progress = 0;
        //if (progress > 100) progress = 100;
        if (!isVisible) return;
        jProgressBar.setValue(progress);
        if (progress <= 25) {
            jProgressBar.setForeground(Color.RED);
        } else if (progress > 25 && progress < 75) {
            jProgressBar.setForeground(Color.BLUE);
        } else if (progress >= 75) {
            jProgressBar.setForeground(Color.GREEN);
        }
//        int decimal = progress / 100;
//        int unit = progress % 100;
        jProgressBar.setString(progress + "%");
        //jProgressBar.setString(decimal + "." + unit + "%");
    }

    private String[][] getStateData(StateInfo stateInfo) {
        if (!isVisible) return null;
        String[] keys = stateInfo.getFormattedKeys();
        String[] vals = stateInfo.getFormattedValues();

        String[][] dataValues = new String[keys.length][2];

        for (int i = 0; i < keys.length; i++) {
            dataValues[i][0] = keys[i];
            dataValues[i][1] = vals[i];
        }
        return dataValues;
    }

    private void setActionListeners() {
        for (int i = 0; i < numMachines; i++) {
            jSpinners[i].addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    JSpinner jSpinner = (JSpinner) e.getSource();
                    changeThrottleValue(Integer.parseInt(jSpinner.getName()), (double) jSpinner.getValue());
                }
            });
        }
    }

    private void moveAnimation(int machineId) throws InterruptedException {
        int x = jFiles[machineId].getLocation().x, y = jFiles[machineId].getLocation().y;
        int finalX = jFiles[(machineId + 1)%numMachines].getLocation().x, finalY = jFiles[(machineId + 1)%numMachines].getLocation().y;
        int currentX = x, currentY = y;
        tempFile.setLocation(x, y);
        jFrame.add(tempFile, 1, 0);

        if (y < finalY) {
            System.out.println("Y above");
            while (currentY < finalY) {
                currentY = currentY + 10;
                if (currentY < (y + (finalY - y) / 2))
                    currentX = currentX + 5;
                else
                    currentX = currentX - 5;
                tempFile.setLocation(currentX, currentY);
                Thread.sleep(10);
            }
        } else {
            System.out.println("Y below");
            while (currentY > finalY) {
                currentY = currentY - 10;
                if (currentY > (finalY + (y - finalY) / 2))
                    currentX = currentX + 5;
                else
                    currentX = currentX - 5;
                tempFile.setLocation(currentX, currentY);
                Thread.sleep(10);
            }
        }
        tempFile.setLocation(finalX, finalY);
        Thread.sleep(100);
        jFrame.remove(tempFile);
    }

    protected void addMessage(Message msg) {
        messageQueue.add(msg);
    }

    private void setThrottleValues(int machineId, double value) {
        if (!isVisible) return;
        jSpinners[machineId].setValue(value);
    }

    private void updateUI() throws InterruptedException {
        Message msg = messageQueue.take();
        if (!isVisible)
            return;
        System.out.println("UI has got a message " + msg);
        double progress = 0.0;
        Long prg = 0l;
        switch (msg.getMsgType()) {
            case Progress:
                System.out.println("Progress Update");
                progress = (double) msg.getData();
                prg = Math.round(progress);
                setProgress(prg.intValue());
                break;
            case SM:
                System.out.println("State Update");
                setState(msg.getMachineId(), (StateInfo) msg.getData());
                break;
            case UITVALUE:
                System.out.println("Updating throttle value " + msg);
                setThrottleValues(msg.getMachineId(), (double) msg.getData());
                break;
            case ResultProgress:
                System.out.println("Result Progress Update");
                progress = (double) msg.getData();
                prg = Math.round(progress);
                setResultProgress(prg.intValue());
                break;
            default:
                break;
        }
//        synchronized (MainThread.jobInQueueLock) {
//            if (MainThread.jobsInQueue) {
//                moveAnimation(0);
//            }
//        }
//        synchronized (MainThread.jobInComingLock) {
//            if (MainThread.jobsInComing) {
//                moveAnimation(1);
//            }
//        }

    }

    @Override
    public void run() {
        if (isVisible)
            jFrame.setVisible(true);
        while (!MainThread.STOP_SIGNAL) {
            try {
                updateUI();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        DynamicBalancerUI dbUI = new DynamicBalancerUI(true, "res");
        dbUI.start();
    }

}
