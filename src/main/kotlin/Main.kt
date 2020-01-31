import implementation.ColorImplementation
import implementation.backend.PigpiodBackend
import implementation.mode.BlinkMode
import implementation.mode.RainbowMode
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.core.util.RouteOverviewPlugin
import io.javalin.http.Context
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.config.Configurator
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory
import java.io.*
import java.util.*

val DEFAULT_PROPERTIES = Properties().apply {
    setProperty("pins.red", "0")
    setProperty("pins.green", "0")
    setProperty("pins.blue", "0")
    setProperty("port", "8080")
    setProperty("pigpiod.port", "8888")
    setProperty("pigpiod.ip", "localhost")
    setProperty("start_color", "#ff1e00")
}


fun main(args: Array<String>) {
    val logger = LogManager.getLogger()

    val file = File(args[0])
    logger.info("Properties file: ${file.absoluteFile}")

    val properties = Properties(DEFAULT_PROPERTIES)

    if (file.exists()) {
        properties.load(InputStreamReader(FileInputStream(file)))
    } else {
        DEFAULT_PROPERTIES.store(OutputStreamWriter(FileOutputStream(file)), "Lighting controller properties. PINs are broadcom GPIO numbers")
        logger.warn("Program will exit. You have to edit the properties files for you needs!")
        return
    }

    val color = ColorImplementation(properties, PigpiodBackend(properties))

    val colorController = LightningController(color, properties)
    val modeController = ModeController(color, properties)

    modeController.addMode(RainbowMode())
    modeController.addMode(BlinkMode())

    Javalin.create {
        it.registerPlugin(RouteOverviewPlugin("routes"))
        it.contextPath = "/control"
        it.logIfServerNotStarted = false
    }.routes {
        path("lighting") {
            get(colorController::getCurrentColor)
            post(colorController::setColor)
            path("reset") {
                post(colorController::reset)
            }
            path("mode") {
                get(modeController::getCurrentMode)
                post(modeController::setMode)
                path("start") {
                    post(modeController::start)
                }
                path("stop") {
                    post(modeController::stop)
                }
                path("list") {
                    get(modeController::getModes)
                }
            }
        }
        get(Global::getInfo)
    }.error(404) {
        it.result("404 Not found\nContext-Path is /control")
    }.start(properties.getProperty("port").toInt())
}

object Global {
    fun getInfo(ctx: Context) {
        ctx.result("LedController by se7kn8 (https://github.com/SE7-KN8/ledcontroller)\nAvailable routes are under /control/routes")
    }
}