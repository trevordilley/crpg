package com.ancient.game.crpg

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import org.slf4j.Logger
import org.slf4j.LoggerFactory


fun gameLogger(clazz: Class<*>): Logger {
    val context = LoggerFactory.getILoggerFactory() as LoggerContext

    val encoder = PatternLayoutEncoder().apply {
        setContext(context)
        pattern = "%msg\t\t\t\t\t\t\t\t%logger{36} %-5level %n"
        start()
    }

    val consoleAppender = ConsoleAppender<ILoggingEvent>().apply {
        setContext(context)
        name = "console"
        setEncoder(encoder)
        start()
    }

    return context.getLogger(clazz).apply {
        isAdditive = false
        addAppender(consoleAppender)
    }
}