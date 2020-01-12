import Vue from 'vue'
import vuetify from '@/plugins/vuetify' // path to vuetify export
var app = new Vue({
    vuetify,
    el: '#app',
    data: {
        errors: [],
        temp: null,
        tempUnit: null,
        humidity: null,
        timestamp: null,
        sensor: null,
        sensors: ["sonoffWaedi"],
        scope: null,
        scopes: ["last 30 days", "last 12 month"]

    },
    watch: {
        sensor: function (val) {
            var me = this;
            minAjax({
                url:"/data/latest",//request URL
                type:"GET",//Request type GET/POST
                //Send Data in form of GET/POST
                data:{
                    sensorName: me.sensor
                },
                debugLog: true,
                //CALLBACK FUNCTION with RESPONSE as argument
                success: function(response){
                    var result = JSON.parse(response);
                    me.tempUnit = result.TempUnit;
                    me.temp = result.AM2301.Temperature.toFixed(1);
                    me.humidity = result.AM2301.Humidity.toFixed(1);
                    me.timestamp = result.Time;
                    // alert(data);
                },
                errorCallback: function() {
                    console.log('PENG!')
                }

            });

        },
        scope: function (val) {
            var me = this;
            minAjax({
                url:"/data/timeseries",//request URL
                type:"GET",//Request type GET/POST
                //Send Data in form of GET/POST
                data:{
                    scope: me.scope,
                    sensorName: me.sensor
                },
                debugLog: true,
                //CALLBACK FUNCTION with RESPONSE as argument
                success: function(response){
                    var result = JSON.parse(response);
                    var ctx = document.getElementById('myChart')
                    var myChart = new Chart(ctx, {
                        type: 'line',
                        data: {
                            labels: result.map(x => x.Time),
                            datasets: [
                                {
                                    label: 'Temperature in °C',
                                    data: result.map(x => x.AM2301.Temperature),
                                    borderColor: '#0066ff',
                                    fill: false,
                                    backgroundColor:'#0066ff',
                                    // borderWidth: 1
                                },
                                // {
                                //     label: 'Low Temperature in °C',
                                //     data: result.map(x => x.AM2301.Temperature - 5),
                                //     borderColor: 'rgba(27,66,255,0.75)',
                                //     fill: origin,
                                //     // borderWidth: 1
                                // },
                                // {
                                // label: 'Medium Temperature in °C',
                                // data: result.map(x => x.temperature),
                                // fill: false,
                                // borderColor: 'rgba(25,255,42,0.95)',
                                // // borderWidth: 1
                                // },
                                // {
                                // label: 'High Temperature in °C',
                                // data: result.map(x => x.temperature + 5),
                                // borderColor: 'rgb(255,0,26)',
                                // backgroundColor: 'rgba(255,77,49,0.27)',
                                // fill: 0
                                // // borderWidth: 1
                                // },
                            ]
                        },
                        options: {
                            maintainAspectRatio: false,
                            layout: {
                                padding: {
                                    left: 10,
                                    right: 10,
                                    top: 10,
                                    bottom: 10
                                }
                            },
                            scales: {
                                yAxes: [{
                                    ticks: {
                                        beginAtZero: true
                                    }
                                }]
                            }
                        }
                    });





                },

                errorCallback: function() {
                    console.log('PENG!')
                }

            });

        }
    },
    created: function () {
        // `this` points to the vm instance
        console.log('a is: ' + this.a)
    }
}).$mount('#app');

