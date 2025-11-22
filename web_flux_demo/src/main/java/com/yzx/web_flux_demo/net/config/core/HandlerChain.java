package com.yzx.web_flux_demo.net.config.core;

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

    private final List<Handler> handlers;
    private final Handler finalHandler; // 最终的业务处理器 (已经适配过的)
    private int index = 0; // 当前执行到的处理器索引

    public HandlerChain(List<Handler> middleware, Handler finalHandler) {
        // 创建一个包含所有中间件和最终处理器的列表
        this.handlers = middleware; // 注意：这里直接引用列表，如果外部修改了列表，这里也会变
        this.finalHandler = finalHandler;
    }

    /**
     * 执行下一个处理器
     * @param context 上下文对象
     */
    public void next(Context context) {
        if (index < handlers.size()) {
            // 执行当前索引的中间件
            try {
                handlers.get(index++).handle(context);
            } catch (Exception e) {
                // 这里可以添加错误处理逻辑，比如记录日志或调用全局错误处理器
                System.err.println("Error in middleware: " + e.getMessage());
                e.printStackTrace();
                // 可以选择中断链或继续，这里选择中断并记录
                // 如果需要更复杂的错误处理，可以在这里扩展
            }
        } else if (index == handlers.size()) {
            // 所有中间件执行完毕，执行最终处理器
            try {
                finalHandler.handle(context);
            } catch (Exception e) {
                // 这里处理最终处理器的异常
                System.err.println("Error in final handler: " + e.getMessage());
                e.printStackTrace();
            }
            index++; // 确保后续调用 next 不会再执行
        }
        // 如果 index > handlers.size()，说明链已经执行完毕，next() 不做任何事
    }
}
