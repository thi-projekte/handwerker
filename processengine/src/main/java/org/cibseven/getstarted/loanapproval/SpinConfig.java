package org.cibseven.getstarted.loanapproval;

import org.cibseven.spin.plugin.impl.SpinProcessEnginePlugin;
import org.springframework.stereotype.Component;

@Component
public class SpinConfig {

    public SpinProcessEnginePlugin plugin(){
        return new SpinProcessEnginePlugin();
    }
}