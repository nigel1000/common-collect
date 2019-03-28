package com.common.collect.container;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Created by hznijianfeng on 2018/8/15. 事务细粒度控制，控制到某一业务段
 */

@Service
@Slf4j
public class TransactionHelper {

    @Transactional
    public void aroundBiz(Runnable biz) {
        biz.run();
    }

    // 提供返回
    @Transactional
    public <T> T aroundBiz(Supplier<T> biz) {
        return biz.get();
    }

    public void afterCommit(@NonNull String taskName, Runnable biz) {
        AfterTransactionCommitExecutor.SingletonInstance.INSTANCE.executeAfterCommit(taskName, biz);
    }

    @Slf4j
    private static class AfterTransactionCommitExecutor extends TransactionSynchronizationAdapter {
        private AfterTransactionCommitExecutor() {
        }

        private static class SingletonInstance {
            private static final AfterTransactionCommitExecutor INSTANCE = new AfterTransactionCommitExecutor();
        }

        private final ThreadLocal<List<Runnable>> runnableTasks = new ThreadLocal<>();
        private final ThreadLocal<List<String>> taskNames = new ThreadLocal<>();

        private void executeAfterCommit(String taskName, Runnable biz) {
            if (biz == null) {
                return;
            }
            if (!TransactionSynchronizationManager.isSynchronizationActive()) {
                log.info("transaction synchronization is not active. executing right now runnable! taskName:{}",
                        taskName);
                ThreadPoolUtil.exec(biz);
                return;
            }
            List<Runnable> runnableList = runnableTasks.get();
            List<String> taskNameList = taskNames.get();
            if (runnableList == null) {
                runnableList = new ArrayList<>();
                taskNameList = new ArrayList<>();
                runnableTasks.set(runnableList);
                taskNames.set(taskNameList);
                TransactionSynchronizationManager.registerSynchronization(this);
            }
            runnableList.add(biz);
            taskNameList.add(taskName);
        }

        @Override
        public void afterCommit() {
            List<Runnable> runnableList = runnableTasks.get();
            List<String> taskNameList = taskNames.get();
            log.info("transaction successfully committed, executing taskNames:{}", taskNameList);
            for (int i = 0; i < runnableList.size(); i++) {
                Runnable biz = runnableList.get(i);
                log.info("executing taskName:{}", taskNameList.get(i));
                try {
                    ThreadPoolUtil.exec(biz);
                } catch (Exception ex) {
                    log.error("failed to execute taskName:{}, Exception :", taskNameList.get(i), ex);
                }
            }
        }

        //  在 afterCommit 之后执行，哪怕 afterCommit 抛出异常也会执行
        @Override
        public void afterCompletion(int status) {
            log.info("transaction completed with status {}", status == STATUS_COMMITTED ? "COMMITTED" : "ROLLED_BACK");
            runnableTasks.remove();
            taskNames.remove();
        }
    }
}
