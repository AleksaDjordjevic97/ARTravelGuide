package com.aleksadjordjevic.augmentedtravelguide.models

import com.google.firebase.firestore.GeoPoint

class User()

{
    var userID:String = ""
    var email:String = ""
    var organization_name:String = ""
    var phone:String = ""
    var profile_image:String = ""


    constructor(
        userID:String,
        email:String,
        organization_name:String,
        phone:String,
        profile_image:String,
    ) : this()

}