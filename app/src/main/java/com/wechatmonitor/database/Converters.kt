package com.wechatmonitor.database

import androidx.room.TypeConverter
import com.wechatmonitor.model.AnalysisMethod

/**
 * Room类型转换器
 */
class Converters {

    /**
     * AnalysisMethod 转 String
     */
    @TypeConverter
    fun fromAnalysisMethod(method: AnalysisMethod): String {
        return method.name
    }

    /**
     * String 转 AnalysisMethod
     */
    @TypeConverter
    fun toAnalysisMethod(value: String): AnalysisMethod {
        return try {
            AnalysisMethod.valueOf(value)
        } catch (e: IllegalArgumentException) {
            AnalysisMethod.NONE
        }
    }
}
