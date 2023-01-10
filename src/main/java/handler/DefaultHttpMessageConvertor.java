package handler;

import com.alibaba.fastjson.JSON;
import log.MyLogger;
import model.MyHttpRequest;

import java.io.IOException;
import java.lang.reflect.Field;

/**
 * 默认的消息转换器，将请求体转换为实体bean
 * 目前简单实现了json和form格式
 *
 * @author fzk
 * @datetime 2023-01-10 14:54:19
 */
public class DefaultHttpMessageConvertor<T> implements HttpMessageConvertor<T> {
    public static final String ContentTypeKey = "Content-Type";
    public static final String ApplicationJson = "application/json";
    public static final String FormUrlencoded = "application/x-www-form-urlencoded";
    private final Class<T> toType;

    public DefaultHttpMessageConvertor(Class<T> clazz) {
        toType = clazz;
    }

    @Override
    public T convert(MyHttpRequest request) throws IOException {
        String contentType = request.getHeaders().get(ContentTypeKey);
        if (contentType == null) contentType = FormUrlencoded;

        T t = null;
        switch (contentType) {
            case ApplicationJson: {
                String line;
                StringBuilder sb = new StringBuilder();
                while ((line = request.getBody().readLine()) != null) {
                    sb.append(line);
                }
                t = JSON.parseObject(sb.toString(), toType);
                break;
            }
            case FormUrlencoded: {
                String line;
                try {
                    t = toType.getDeclaredConstructor().newInstance();

                    while ((line = request.getBody().readLine()) != null) {
                        String[] args = line.split("&");
                        for (String arg : args) {
                            String[] arr = arg.split("=");
                            String key = arr[0];
                            String val = arr[1];
                            Field field = getFieldSafely(key, toType);
                            field.setAccessible(true);
                            typeConvert(t, field, val);
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                break;
            }
        }
        return t;
    }

    private Field getFieldSafely(String fieldName, Class<?> clazz) {
        Field field = null;
        try {
            field = clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            MyLogger.logger.warning(String.format("%s没有找到字段: %s", clazz, fieldName));
        }
        return field;
    }

    private void typeConvert(T t, Field field, String val) throws Exception {
        Class<?> fieldType = field.getType();
        if (fieldType == String.class) {
            field.set(t, val);
        } else if (fieldType == int.class || fieldType == Integer.class) {
            field.set(t, Integer.parseInt(val));
        } else if (fieldType == int[].class) {
            int[] old = (int[]) field.get(t);
            if (old == null) {
                field.set(t, new int[]{Integer.parseInt(val)});
            } else {
                int[] newArr = new int[old.length + 1];
                System.arraycopy(old, 0, newArr, 0, old.length);
                newArr[newArr.length - 1] = Integer.parseInt(val);
                field.set(t, newArr);
            }
        } else {
            throw new RuntimeException(String.format("蚌埠住了, 暂不支持该类型: %s, 目前仅支持String, int", fieldType));
        }
    }
}
