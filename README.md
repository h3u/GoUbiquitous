# Go Ubiquitous

This project adds an Android Wear Watchface to the given Weather App ["Sunshine"](https://github.com/udacity/Advanced_Android_Development/tree/7.05_Pretty_Wallpaper_Time) (Udacity Nanodegree Project 6).

Implemented enhancements:

- Digital Watchface displaying time, date, weekday, high-/low-Temperature and a weather indicator icon
- additional update of the wearable via `DataApi` using data items when the app synchronizes the weather data
- storage of received weather data from handheld to `SharedPreferences`
- request for synchronization from wearable to handheld using messages with `CapabilityApi` and `MessageApi`


## Screenshots


![Screenshot of square Watchface](Screenshot_Watchface_Square.png)
![Screenshot of square Watchface in ambient mode](Screenshot_Watchface_Square_Ambient.png)

![Screenshot of round Watchface with chin](Screenshot_Watchface_Round_Chin.png)
![Screenshot of round Watchface with chin in ambient mode](Screenshot_Watchface_Round_Chin_Ambient.png)

![Screenshot of round Watchface](Screenshot_Watchface_Round.png)
![Screenshot of round Watchface in ambient mode](Screenshot_Watchface_Round_Ambient.png)



## Configuration

Add your personal api key for [http://openweathermap.org/](http://openweathermap.org/appid) to
`local.properties` before building the app.

Please add following line:
`MyOpenWeatherMapApiKey="<MY_PERSONAL_API_KEY>"`