//package org.cinemind.converter
//
//import jakarta.persistence.AttributeConverter
//import jakarta.persistence.Converter
//
//@Converter
//class VectorConverter : AttributeConverter<List<Float>, String>{
//    override fun convertToDatabaseColumn(attribute: List<Float>?): String? {
//        return attribute?.joinToString (prefix = "[", postfix = "]") ?: "[]"
//    }
//
//    override fun convertToEntityAttribute(dbData: String?): List<Float>? {
//        if (dbData.isNullOrBlank()) return emptyList()
//            return dbData
//                .replace("[", "")
//                .replace("]", "")
//                .split(",")
//                .mapNotNull { it.trim().toFloatOrNull() }
//    }
//}
