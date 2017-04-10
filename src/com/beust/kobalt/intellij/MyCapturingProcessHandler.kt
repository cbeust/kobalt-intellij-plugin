package com.beust.kobalt.intellij

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.diagnostic.Logger

/**
 * @author Dmitry Zhuravlev
 *         Date:  13.05.2016
 */
class MyCapturingProcessHandler(commandLine: GeneralCommandLine) : CapturingProcessHandler(commandLine) {
    companion object{
        private val LOG = Logger.getInstance(MyCapturingProcessHandler::class.java)
    }

    override fun killProcessTree(process: Process) {
        executeOnPooledThread { doDestroy(process) }
    }

    private fun doDestroy(process: Process) {
        LOG.debug("destroying process...")
        if (!process.isAlive) {
            LOG.warn("Process has been already terminated: " + this.myCommandLine)
        } else {
            LOG.debug("Trying to destroy process using Java API. Cmdline:\n" + this.myCommandLine)
            process.destroy()
        }
    }
}