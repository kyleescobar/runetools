package dev.runetools.asm.util

import me.tongfei.progressbar.ProgressBar
import me.tongfei.progressbar.ProgressBarBuilder
import me.tongfei.progressbar.ProgressBarStyle
import java.time.temporal.ChronoUnit

object ConsoleProgressBar {

    private lateinit var progressBar: ProgressBar

    fun enable(taskName: String, initialMax: Int) {
        progressBar = ProgressBarBuilder()
            .setTaskName("Mapping $taskName")
            .setStyle(ProgressBarStyle.COLORFUL_UNICODE_BAR)
            .setInitialMax(initialMax.toLong())
            .setUpdateIntervalMillis(100)
            .setSpeedUnit(ChronoUnit.SECONDS)
            .showSpeed()
            .setMaxRenderedLength(200)
            .build()
    }

    fun disable() {
        progressBar.close()
    }

    fun step() {
        progressBar.step()
    }

    fun stepTo(step: Int) {
        progressBar.stepTo(step.toLong())
    }

    fun stepBy(steps: Int) {
        progressBar.stepBy(steps.toLong())
    }

    fun hint(message: String) {
        progressBar.extraMessage = message
    }
}