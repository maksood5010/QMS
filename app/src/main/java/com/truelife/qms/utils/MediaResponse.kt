package com.truelife.qms.utils

data class MediaResponse(
	val duration: Int,
	val media_element: List<File>,
	val last_updated: String,
	val institution_name: String
)

data class File(
	val media_type: String,
	val url: String
){
	/**
	 * video / image
	 * */
}
