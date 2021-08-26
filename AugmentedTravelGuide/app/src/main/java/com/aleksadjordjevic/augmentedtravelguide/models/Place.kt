package com.aleksadjordjevic.augmentedtravelguide.models

import com.google.firebase.firestore.GeoPoint

class Place()

{
    var id:String = ""
    var name:String = ""
    var description:String = ""
    var geoPoint:GeoPoint = GeoPoint(0.0, 0.0)
    var image_for_scanning:String = ""
    var model_for_ar:String = ""


    constructor(
        id:String,
        name:String,
        description:String,
        geoPoint:GeoPoint,
        image_for_scanning:String,
        model_for_ar:String,
    ) : this()

}