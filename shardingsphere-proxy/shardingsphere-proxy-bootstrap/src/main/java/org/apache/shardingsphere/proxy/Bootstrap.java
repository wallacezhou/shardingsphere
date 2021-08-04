/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.proxy;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.shardingsphere.governance.core.rule.GovernanceRule;
import org.apache.shardingsphere.governance.core.yaml.swapper.GovernanceConfigurationYamlSwapper;
import org.apache.shardingsphere.governance.repository.api.config.GovernanceConfiguration;
import org.apache.shardingsphere.infra.config.RuleConfiguration;
import org.apache.shardingsphere.infra.config.persist.repository.DistMetaDataPersistRepositoryFactory;
import org.apache.shardingsphere.infra.rule.persist.DistMetaDataPersistRuleConfiguration;
import org.apache.shardingsphere.infra.yaml.config.swapper.YamlRuleConfigurationSwapperEngine;
import org.apache.shardingsphere.proxy.arguments.BootstrapArguments;
import org.apache.shardingsphere.proxy.config.ProxyConfigurationLoader;
import org.apache.shardingsphere.proxy.config.YamlProxyConfiguration;
import org.apache.shardingsphere.proxy.initializer.BootstrapInitializer;
import org.apache.shardingsphere.proxy.initializer.impl.GovernanceBootstrapInitializer;
import org.apache.shardingsphere.proxy.initializer.impl.StandardBootstrapInitializer;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Properties;

/**
 * ShardingSphere-Proxy Bootstrap.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Bootstrap {
    
    /**
     * Main entrance.
     *
     * @param args startup arguments
     * @throws IOException IO exception
     * @throws SQLException SQL exception
     */
    public static void main(final String[] args) throws IOException, SQLException {
        BootstrapArguments bootstrapArgs = new BootstrapArguments(args);
        YamlProxyConfiguration yamlConfig = ProxyConfigurationLoader.load(bootstrapArgs.getConfigurationPath());
        createBootstrapInitializer(yamlConfig).init(yamlConfig, bootstrapArgs.getPort());
    }
    
    private static BootstrapInitializer createBootstrapInitializer(final YamlProxyConfiguration yamlConfig) {
        if (null == yamlConfig.getServerConfiguration().getGovernance()) {
            return new StandardBootstrapInitializer(DistMetaDataPersistRepositoryFactory.newInstance(findDistMetaDataPersistRuleConfiguration(yamlConfig)));
        }
        // TODO create GovernanceRule from SPI
        GovernanceRule governanceRule = new GovernanceRule(findGovernanceConfiguration(yamlConfig));
        return new GovernanceBootstrapInitializer(governanceRule);
    }
    
    private static DistMetaDataPersistRuleConfiguration findDistMetaDataPersistRuleConfiguration(final YamlProxyConfiguration yamlConfig) {
        Collection<RuleConfiguration> ruleConfigs = new YamlRuleConfigurationSwapperEngine().swapToRuleConfigurations(yamlConfig.getServerConfiguration().getRules());
        return ruleConfigs.stream().filter(each -> each instanceof DistMetaDataPersistRuleConfiguration)
                .map(each -> (DistMetaDataPersistRuleConfiguration) each).findFirst().orElse(new DistMetaDataPersistRuleConfiguration("Local", true, new Properties()));
    }
    
    private static GovernanceConfiguration findGovernanceConfiguration(final YamlProxyConfiguration yamlConfig) {
        return new GovernanceConfigurationYamlSwapper().swapToObject(yamlConfig.getServerConfiguration().getGovernance());
    }
}
