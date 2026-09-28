package com.nivora.browser.privacy

import java.security.SecureRandom
import kotlin.math.ln

data class PasswordOptions(
    val length: Int = 16,
    val includeUppercase: Boolean = true,
    val includeLowercase: Boolean = true,
    val includeNumbers: Boolean = true,
    val includeSymbols: Boolean = true
)

enum class PasswordStrength(val label: String) {
    WEAK("Weak"),
    MODERATE("Moderate"),
    STRONG("Strong"),
    VERY_STRONG("Very Strong")
}

object PasswordGenerator {

    private const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
    private const val NUMBERS = "0123456789"
    private const val SYMBOLS = "!@#$%^&*()_+-=[]{}|;:,.<>?"

    private val secureRandom = SecureRandom()

    fun generatePassword(options: PasswordOptions): String {
        val pool = StringBuilder()
        val guaranteedChars = mutableListOf<Char>()

        if (options.includeUppercase) {
            pool.append(UPPERCASE)
            guaranteedChars.add(UPPERCASE[secureRandom.nextInt(UPPERCASE.length)])
        }
        if (options.includeLowercase) {
            pool.append(LOWERCASE)
            guaranteedChars.add(LOWERCASE[secureRandom.nextInt(LOWERCASE.length)])
        }
        if (options.includeNumbers) {
            pool.append(NUMBERS)
            guaranteedChars.add(NUMBERS[secureRandom.nextInt(NUMBERS.length)])
        }
        if (options.includeSymbols) {
            pool.append(SYMBOLS)
            guaranteedChars.add(SYMBOLS[secureRandom.nextInt(SYMBOLS.length)])
        }

        if (pool.isEmpty()) {
            pool.append(LOWERCASE)
            guaranteedChars.add(LOWERCASE[secureRandom.nextInt(LOWERCASE.length)])
        }

        val poolString = pool.toString()
        val remainingLength = (options.length - guaranteedChars.size).coerceAtLeast(0)
        val passwordChars = ArrayList<Char>(options.length)
        passwordChars.addAll(guaranteedChars)

        for (i in 0 until remainingLength) {
            val randomChar = poolString[secureRandom.nextInt(poolString.length)]
            passwordChars.add(randomChar)
        }

        // Fisher-Yates shuffle with SecureRandom
        for (i in passwordChars.size - 1 downTo 1) {
            val j = secureRandom.nextInt(i + 1)
            val temp = passwordChars[i]
            passwordChars[i] = passwordChars[j]
            passwordChars[j] = temp
        }

        return passwordChars.joinToString("")
    }

    fun calculateEntropyBits(password: String): Double {
        if (password.isEmpty()) return 0.0
        var poolSize = 0
        if (password.any { it.isUpperCase() }) poolSize += UPPERCASE.length
        if (password.any { it.isLowerCase() }) poolSize += LOWERCASE.length
        if (password.any { it.isDigit() }) poolSize += NUMBERS.length
        if (password.any { SYMBOLS.contains(it) }) poolSize += SYMBOLS.length

        if (poolSize == 0) poolSize = 26
        return password.length * (ln(poolSize.toDouble()) / ln(2.0))
    }

    fun getStrength(entropyBits: Double): PasswordStrength {
        return when {
            entropyBits < 40.0 -> PasswordStrength.WEAK
            entropyBits < 60.0 -> PasswordStrength.MODERATE
            entropyBits < 80.0 -> PasswordStrength.STRONG
            else -> PasswordStrength.VERY_STRONG
        }
    }
}
