package com.yzx.crazycodingbytecommon.aop;

import com.yzx.crazycodingbytecommon.entity.BusinessException;
import com.yzx.crazycodingbytecommon.entity.Idempotent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.aspectj.lang.reflect.SourceLocation;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * @className: IdempotentAspect
 * @author: yzx
 * @date: 2026/1/9 6:04
 * @Version: 1.0
 * @description:
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class IdempotentAspect {
    private final StringRedisTemplate redisTemplate;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        // 1. 获取注解属性
        String keyExpression = idempotent.key();
        long expireTime = idempotent.expireTime();
        TimeUnit timeUnit = idempotent.timeUnit();
        String errorMessage = idempotent.message();
        //2解析spl表达式生成唯一的幂值
        String idempotentKey = parseSpel(keyExpression, joinPoint);
        if (idempotentKey == null || idempotentKey.isEmpty()) {
            throw new BusinessException("幂等键解析失败，请检查@Idempotent的key表达式");
        }
        log.info("解析幂等键成功，key：{}", idempotentKey);
        Boolean isFirstExecute = redisTemplate.opsForValue().setIfAbsent(idempotentKey, "1", expireTime, timeUnit);
        //幂等键已存在，说明已经执行过
        if (Boolean.FALSE.equals(isFirstExecute)) {
            log.warn("重复执行幂等方法，幂等键：{}", idempotentKey);
            throw new BusinessException(errorMessage);
        }
        // 5. 第一次执行：放行，执行原方法
        try {
            return joinPoint.proceed(); // 执行业务方法
        } catch (Exception e) {
            // 6. 业务执行失败：删除幂等键，允许重试
            log.error("业务方法执行失败，删除幂等键：{}", idempotentKey, e);
            redisTemplate.delete(idempotentKey);
            throw e; // 抛出异常，让上层处理
        }
    }

    /**
     * 解析SpEL表达式
     *
     * @param expression 表达式
     * @param joinPoint  连接点
     * @return 解析结果
     */
    private String parseSpel(String expression, ProceedingJoinPoint joinPoint) {
        //获取目标方法
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        String kind = joinPoint.getKind();
        log.info("kind:{}", kind);
        Object target = joinPoint.getTarget();
        log.info("target:{}", target);
        SourceLocation sourceLocation = joinPoint.getSourceLocation();
        log.info("sourceLocation:{}", sourceLocation);
        Object aThis = joinPoint.getThis();
        log.info("aThis:{}", aThis);
        //构建SpEl上下文
        StandardEvaluationContext standardEvaluationContext = new StandardEvaluationContext();
        //获取参数名称和参数值
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
        //获取参数值
        Object[] args = joinPoint.getArgs();
        if (parameterNames != null && args != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                standardEvaluationContext.setVariable(parameterNames[i], args[i]);
            }
        }
        /**
         * // 有3个参数的方法
         * @Idempotent(key = "'TEST_'+#userId+'_'+#lockDTO.orderNo+'_'+#productId", message = "重复操作")
         * public void testMethod(String userId, InventoryLockDTO lockDTO, String productId) {
         *     // userId = "u1001"
         *     // lockDTO = InventoryLockDTO(orderNo="20260109", quantity=5)
         *     // productId = "p2001"
         * }
         *
         * // parameterNames = ["userId", "lockDTO", "productId"]
         * // args = ["u1001", lockDTO对象, "p2001"]
         * for (int i = 0; i < parameterNames.length; i++) {
         *     context.setVariable(parameterNames[i], args[i]);
         *     // 执行后：
         *     // context里有：userId→u1001，lockDTO→lockDTO对象，productId→p2001
         * }
         * 会根据注解的属性拼接幂等键
         */
        // 解析SpEL表达式，返回字符串结果
        return expressionParser.parseExpression(expression).getValue(standardEvaluationContext, String.class);
    }
}
