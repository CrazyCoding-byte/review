package com.yzx.crazycodingbytehttp.config.core;

import java.util.ArrayList;
import java.util.List;

/**
 * @className: HandlerChain
 * @author: yzx
 * @date: 2025/11/23 0:36
 * @Version: 1.0
 * @description:
 */
public class HandlerChain {
    // 修改：将中间件列表和最终处理器分开存储
    private final List<Handler> handlers; // 包含路由中间件和最终适配后的处理器
    private int index = 0; // 当前执行到的处理器索引

    // 修改构造函数：接收一个包含所有 Handler (中间件 + 最终处理器) 的列表
    public HandlerChain(List<Handler> handlers) {
        this.handlers = handlers; // 创建副本以防止外部修改
    }

    /**
     * 执行下一个处理器
     * @param context 上下文对象
     */
    public void next(Context context) {
        if (index < handlers.size()) {
            // 执行当前索引的处理器 (中间件或最终处理器)
            try {
                handlers.get(index++).handle(context);
            } catch (Exception e) {
                // 这里可以添加错误处理逻辑，比如记录日志或调用全局错误处理器
                System.err.println("Error in handler (middleware or final): " + e.getMessage());
                e.printStackTrace();
                // 可以选择中断链或继续，这里选择中断并记录
                // 如果需要更复杂的错误处理，可以在这里扩展
            }
        }
        // 如果 index >= handlers.size()，说明链已经执行完毕，next() 不做任何事
    }
}
