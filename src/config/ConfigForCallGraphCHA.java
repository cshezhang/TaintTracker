package config;


import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.config.IInfoflowConfig;
import soot.options.Options;

import java.util.LinkedList;
import java.util.List;

public class ConfigForCallGraphCHA implements IInfoflowConfig {
    @Override
    public void setSootOptions(Options options, InfoflowConfiguration config) {
        List<String> excludeList = new LinkedList<>();
        excludeList.add("java.*");
        excludeList.add("sun.*");
        excludeList.add("android.*");
        excludeList.add("org.apache.*");
        excludeList.add("org.eclipse.*");
        excludeList.add("soot.*");
        excludeList.add("javax.*");
        options.set_exclude(excludeList);
        Options.v().set_no_bodies_for_excluded(true);
        Options.v().set_process_multiple_dex(false);
        options.set_output_format(Options.output_format_none);
        config.setCallgraphAlgorithm(InfoflowConfiguration.CallgraphAlgorithm.CHA);
        config.setCodeEliminationMode(InfoflowConfiguration.CodeEliminationMode.NoCodeElimination);
    }
}
