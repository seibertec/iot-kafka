var app = new Vue({
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
                data:{},
                debugLog: true,
                //CALLBACK FUNCTION with RESPONSE as argument
                success: function(response){
                    var result = JSON.parse(response);
                    me.temp = result.temperature;
                    me.tempUnit = result.tempUnit;
                    me.humidity = result.humidity;
                    me.timestamp = result.timestamp;
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
                    scope: me.scope
                },
                debugLog: true,
                //CALLBACK FUNCTION with RESPONSE as argument
                success: function(response){
                    var result = JSON.parse(response);
                    var ctx = document.getElementById('myChart');
                    var myChart = new Chart(ctx, {
                        type: 'bar',
                        data: {
                            labels: result.map(x => x.timestamp),
                            datasets: [{
                                label: 'Temperature in °C',
                                data: result.map(x => x.temperature),
                                // backgroundColor: [
                                //     'rgba(255, 99, 132, 0.2)',
                                //     'rgba(54, 162, 235, 0.2)',
                                //     'rgba(255, 206, 86, 0.2)',
                                //     'rgba(75, 192, 192, 0.2)',
                                //     'rgba(153, 102, 255, 0.2)',
                                //     'rgba(255, 159, 64, 0.2)'
                                // ],
                                // borderColor: [
                                //     'rgba(255, 99, 132, 1)',
                                //     'rgba(54, 162, 235, 1)',
                                //     'rgba(255, 206, 86, 1)',
                                //     'rgba(75, 192, 192, 1)',
                                //     'rgba(153, 102, 255, 1)',
                                //     'rgba(255, 159, 64, 1)'
                                // ],
                                borderWidth: 1
                            }]
                        },
                        options: {
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
});
