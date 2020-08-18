package util;

import config.ConfigForAnalyzingCG;
import config.ConfigForAndroidMultipleDex;
import config.ConfigForCallGraphCHA;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.config.IInfoflowConfig;
import test.MainTest;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

public class CG_Generator {

    public static String CHA_PATH;
    public static ArrayList<String> CHA_List = new ArrayList<>();

    public static void constructCHAList() {
        try {
            FileReader reader = new FileReader(CHA_PATH);
            BufferedReader br = new BufferedReader(reader);
            String str;
            while ((str = br.readLine()) != null) {
                CHA_List.add(str);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void constructCallGraphByAnalyzingCG() {
        System.out.println("Begin to Generate Call Graph...");
        SetupApplication application = new SetupApplication(config.Config.androidPlatformPath, config.Config.appPath);
        IInfoflowConfig iInfoflowConfig = new ConfigForAnalyzingCG();
        application.setSootConfig(iInfoflowConfig);
        System.out.println("Configure has been inited!");
        application.constructCallgraph();
        System.out.println("Call Graph has been generated!");
    }


    public static void constructCallGraph() {
        System.out.println("Begin to Generate Call Graph...");
        if (CHA_List.contains(MainTest.pkgName)) {
            SetupApplication application = new SetupApplication(config.Config.androidPlatformPath, config.Config.appPath);
            IInfoflowConfig iInfoflowConfig = new ConfigForCallGraphCHA();
            application.setSootConfig(iInfoflowConfig);
            System.out.println("Configure has been inited!");
            application.constructCallgraph();
        } else {
            SetupApplication application = new SetupApplication(config.Config.androidPlatformPath, config.Config.appPath);
            IInfoflowConfig iInfoflowConfig = new ConfigForAndroidMultipleDex();
            application.setSootConfig(iInfoflowConfig);
            System.out.println("Configure has been inited!");
            application.constructCallgraph();
        }
        System.out.println("Call Graph has been generated!");
    }


}
