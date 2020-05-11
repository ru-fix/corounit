package ru.fix.corounit.allure

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature

@Aspect
class StepAnnotationAspect {
    @Pointcut("@annotation(ru.fix.corounit.allure.Step)")
    fun withStepAnnotation() {
        //pointcut body, should be empty
    }

    @Pointcut("@within(ru.fix.corounit.allure.Step)")
    fun withinStepAnnotation() {
        //pointcut body, should be empty
    }

    @Pointcut("execution(* *(..))")
    fun anyMethod() {
        //pointcut body, should be empty
    }

    @Around("anyMethod() && (withStepAnnotation() || withinStepAnnotation())")
    fun aroundStepMethod(joinPoint: ProceedingJoinPoint): Any? {
        val originMethod = (joinPoint.signature as MethodSignature).method
        val originArgs = joinPoint.args


        val methodWrapper = StepMethodWrapper(originMethod, originArgs)

        return methodWrapper.wrappedInvoke { newArgs ->
            joinPoint.proceed(newArgs)
        }
    }
}