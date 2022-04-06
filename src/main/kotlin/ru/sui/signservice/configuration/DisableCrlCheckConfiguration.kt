package ru.sui.signservice.configuration

import net.bytebuddy.ByteBuddy
import net.bytebuddy.agent.ByteBuddyAgent
import net.bytebuddy.description.NamedElement
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy
import net.bytebuddy.implementation.FixedValue
import net.bytebuddy.matcher.ElementMatchers.named
import net.bytebuddy.matcher.ElementMatchers.takesArguments
import org.springframework.context.annotation.Configuration
import ru.CryptoPro.AdES.certificate.BaseCertificateChainValidatorImpl
import javax.annotation.PostConstruct

@Configuration
class DisableCrlCheckConfiguration {

    @PostConstruct
    fun postConstruct() {
        // Они сказали, что нельзя отключить проверку серта в списках отзыва
        ByteBuddyAgent.install()

        val classReloadingStrategy = ClassReloadingStrategy.fromInstalledAgent()

        ByteBuddy()
            .redefine(BaseCertificateChainValidatorImpl::class.java)
            .method(named<NamedElement>("validate").and(takesArguments(MutableList::class.java, MutableList::class.java)))
            .intercept(FixedValue.originType())
            .make()
            .load(BaseCertificateChainValidatorImpl::class.java.classLoader, classReloadingStrategy)
    }

}