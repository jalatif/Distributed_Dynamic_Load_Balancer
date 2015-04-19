package DLB.Utils;

import com.sun.management.OperatingSystemMXBean;
import com.sun.xml.internal.ws.api.message.Packet;
import org.hyperic.sigar.CpuInfo;
import org.hyperic.sigar.ProcCpu;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import javax.management.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;


/**
 * Created by sam on 4/17/15.
 */
public class SystemStat {

//    public static void main(String[] args) throws MalformedObjectNameException, ReflectionException, InstanceNotFoundException, IOException {
//        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(
//                OperatingSystemMXBean.class);
//        System.out.println("System Load Average: " + osBean.getSystemCpuLoad());
////        System.out.println(getCPUStat());
//        getCPUStat();
//    }

    public static void main(String[] args) {
        Sigar sigar = new Sigar();

        ProcCpu cpu = null;
        long pId = sigar.getPid(); // this one gives me the same process ID that I see in visualVM
        try {
            cpu = sigar.getProcCpu(pId);
        } catch (SigarException se) {
            se.printStackTrace();
        }
        System.out.print(cpu.getPercent());
    }
    public static void getCPUStat() throws IOException, SigarException {
//        Runtime rt = Runtime.getRuntime();
//        String[] commands = {"/home/manshu/Templates/EXEs/CS423/MP4_DLB/test.cpu"};
//        Process proc = rt.exec(commands);
//
//        BufferedReader stdInput = new BufferedReader(new
//                InputStreamReader(proc.getInputStream()));
//
//        BufferedReader stdError = new BufferedReader(new
//                InputStreamReader(proc.getErrorStream()));
//
//        String s = "0";
////        s= stdInput.readLine();
//        while ((s = stdInput.readLine()) != null) {
//            if (s.equals("")) continue;
//            System.out.println("TEST");
//            System.out.println(s);
//        }

//        return Double.parseDouble(s);
//        System.out.println("Here is the standard error of the command (if any):\n");
//        while ((s = stdError.readLine()) != null) {
//            System.out.println(s);
//        }
    }

}
