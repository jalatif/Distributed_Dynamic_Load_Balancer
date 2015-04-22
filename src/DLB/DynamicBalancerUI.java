package DLB;

import DLB.Utils.Message;
import DLB.Utils.MessageType;
import DLB.Utils.StateInfo;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.concurrent.LinkedBlockingQueue;

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
    JLabel[] jLabels;
    JLabel[] jFiles;
    JLabel tempFile;
    JTable[] jTables;
    JScrollPane[] jScrollPanes;
    JFrame jFrame;

    String[] columnNames = {"Property", "Value"};

    public DynamicBalancerUI() {
        messageQueue = new LinkedBlockingQueue<>();
        throttleValues = new double[numMachines];
        machineStates = new StateInfo[numMachines];
        changeLooks();
        initUI();
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

//        String path = MainThread.class.getClass().getResource("DLB/res/images").getPath();
//
//        ImageIcon machine = new ImageIcon(path + "/pc-icon.png");
//        ImageIcon file = new ImageIcon(path + "/file.png");

        ImageIcon machine = new ImageIcon("res/images/pc-icon.png");
        ImageIcon file = new ImageIcon("res/images/file.png");

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

            //table
            jScrollPanes[i] = new JScrollPane();
            jTables[i] = new JTable(getStateData(new StateInfo(0)), columnNames);
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
            jFrame.add(jScrollPanes[i]);
        }
        jProgressBar = new JProgressBar();
        jProgressBar.setStringPainted(true);
        jProgressBar.setPreferredSize(new Dimension(500, 50));
        jProgressBar.setMinimum(0);
        jProgressBar.setMaximum(10000);
        jFrame.add(jProgressBar);
        setProgress(0);

        setActionListeners();
    }

    private void changeLooks() {
        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
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
        String[][] datavalues = getStateData(stateInfo);
        for (int i = 0; i < datavalues.length; i++) {
            jTables[machineId].setValueAt(datavalues[i][0], i, 0);
            jTables[machineId].setValueAt(datavalues[i][1], i, 1);
        }
    }

    protected void setProgress(int progress) {
        //if (progress < 0) progress = 0;
        //if (progress > 100) progress = 100;

        jProgressBar.setValue(progress);
        if (progress <= 2500) {
            jProgressBar.setForeground(Color.RED);
        } else if (progress > 2500 && progress < 7500) {
            jProgressBar.setForeground(Color.BLUE);
        } else if (progress >= 7500) {
            jProgressBar.setForeground(Color.GREEN);
        }
        int decimal = progress / 100;
        int unit = progress % 100;
        jProgressBar.setString(decimal + "." + unit + "%");
    }

    private String[][] getStateData(StateInfo stateInfo) {
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
                    //setProgress(jProgressBar.getValue() + 10);
                }
            });
        }
    }

    private void moveAnimation(int machineId) throws InterruptedException {
        int x = jFiles[machineId].getLocation().x, y = jFiles[machineId].getLocation().y;
        int finalX = jFiles[(machineId + 1)%numMachines].getLocation().x, finalY = jFiles[(machineId + 1)%numMachines].getLocation().y;
        int currentX = x, currentY = y;
        //tempFile.setVisible(true);
        tempFile.setLocation(x, y);
        jFrame.add(tempFile, 1, 0);

        if (x < finalX) {
            while (currentX < finalX) {
                currentX = currentX + 10;
                if (currentX < (x + (finalX - x) / 2))
                    currentY = currentY + 5;
                else
                    currentY = currentY - 5;
                tempFile.setLocation(currentX, currentY);
                Thread.sleep(10);
            }
        } else {
            while (currentX > finalX) {
                currentX = currentX - 10;
                if (currentX > (finalX + (x - finalX) / 2))
                    currentY = currentY + 5;
                else
                    currentY = currentY - 5;
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
        jSpinners[machineId].setValue(value);
    }

    private void updateUI() throws InterruptedException {
        Message msg = messageQueue.take();
        System.out.println("UI has got a message " + msg);
        switch (msg.getMsgType()) {
            case Progress:
                System.out.println("Progress Update");
                double progress = (double) msg.getData();
                Long prg = Math.round(progress);
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
        jFrame.setVisible(true);
        while (!MainThread.STOP_SIGNAL) {
            try {
                updateUI();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        DynamicBalancerUI dbUI = new DynamicBalancerUI();
        dbUI.start();
    }

}
