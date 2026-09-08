package com.milibaby.app

data class BabyProfile(val name:String, val birthDate:String, val weightGrams:Int, val feedingType:String)
data class Feeding(val id:Long, val amountMl:Int, val timeMillis:Long, val note:String)
data class Recommendation(val minMl:Int?, val avgMl:Int?, val maxMl:Int?, val explanation:String, val strictDaily:Boolean)
