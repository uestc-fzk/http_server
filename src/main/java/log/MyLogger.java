package log;

import config.ConfigReader;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.*;

/**
 * 提供者Supplier函数式接口 提供日志信息
 * <p>
 * 7个日志级别, 从上到下依次递减
 * 默认只记录前3个级别
 * <p>
 * todo: 组提交未实现
 *
 * @author fzk
 * @datetime 2021-11-17 10:26
 */
@SuppressWarnings("unused")
public class MyLogger {

    public static final Logger logger = Logger.getLogger("my.logger");

    static {
        // 默认是INFO级别，由于处理器默认是INFO级别，因此要修改，必须连同处理器也一起修改，如下面这种方式
        Level level = Level.parse(ConfigReader.getConfig().getLogLevel().toUpperCase());
        MyFormatter formatter = new MyFormatter();
        logger.setLevel(level);
        logger.setUseParentHandlers(false);

        // 控制台正常输出：过滤高级别异常日志
        MyConsoleHandler h1 = new MyConsoleHandler(level, formatter, System.out);
        h1.setFilter(record -> record.getLevel().intValue() <= Level.INFO.intValue());
        logger.addHandler(h1);
        // 控制台异常输出：过滤低级别正常日志
        MyConsoleHandler h2 = new MyConsoleHandler(level, formatter, System.err);
        h2.setFilter(record -> record.getLevel().intValue() >= Level.WARNING.intValue());
        logger.addHandler(h2);


        // 配置文件日志输出
        try {
            FileHandler fileHandler = new FileHandler(ConfigReader.getConfig().getLogPath(), 1024 * 1024, 5, false);
            fileHandler.setLevel(level);
            fileHandler.setFormatter(formatter);
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // 自定义日志格式
    static class MyFormatter extends SimpleFormatter {
        @Override
        public String format(LogRecord record) {
            String time = MyDateTimeUtil.nowDateTime();
            String source;
            if (record.getSourceClassName() != null) {
                source = record.getSourceClassName();
                if (record.getSourceMethodName() != null) {
                    source += " " + record.getSourceMethodName();
                }
            } else {
                source = record.getLoggerName();
            }
            String message = formatMessage(record);
            String throwable = "";
            if (record.getThrown() != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                pw.println();
                record.getThrown().printStackTrace(pw);
                pw.close();
                throwable = sw.toString();
            }
            return String.format("%s %s %s msg: %s %s\n",
                    record.getLevel().getName(),
                    record.getLoggerName(),
                    time,
//                    source,
                    message,
                    throwable);
        }
    }

    // 从java.util.logging.ConsoleHandler模仿，主要是控制System.out和System.err
    static class MyConsoleHandler extends StreamHandler {
        public MyConsoleHandler(Level level, MyFormatter formatter, PrintStream printStream) {
            super(printStream, formatter);
            setLevel(level);
        }

        @Override
        public synchronized void publish(LogRecord record) {
            super.publish(record);
            super.flush();// 控制台每条日志都flush
        }
    }
}