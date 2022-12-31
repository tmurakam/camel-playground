package org.tmurakam.camel;

import com.atomikos.icatch.jta.UserTransactionImp;
import com.atomikos.icatch.jta.UserTransactionManager;
import org.apache.camel.spring.spi.SpringTransactionPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.transaction.SystemException;

@Configuration
@EnableTransactionManagement
public class TxConfig {
    public static final String TX_POLICY = "txRequiresNew";

    @Bean
    @Primary
    public JtaTransactionManager transactionManager() throws SystemException {
        UserTransactionManager userTransactionManager = new UserTransactionManager();
        userTransactionManager.setForceShutdown(true);

        UserTransactionImp userTransaction = new UserTransactionImp();
        userTransaction.setTransactionTimeout(300);
        
        return new JtaTransactionManager(userTransaction, userTransactionManager);
    }

    @Bean(TX_POLICY)
    public SpringTransactionPolicy transactionPolicy(@Autowired JtaTransactionManager txManager) {
        SpringTransactionPolicy policy = new SpringTransactionPolicy(txManager);
        policy.setTransactionManager(txManager);
        policy.setPropagationBehaviorName("PROPAGATION_REQUIRES_NEW");
        return policy;
    }
}
