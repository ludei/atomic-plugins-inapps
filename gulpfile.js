var gulp = require('gulp'); 
var path = require('path');
var jshint = require('gulp-jshint');
var fs = require('fs');
var shell = require('gulp-shell');
var del = require('del');
var cordova = require('cordova-lib').cordova.raw;
var jsdoc = require('gulp-jsdoc');
var uglify = require('gulp-uglify');

gulp.task('clean', function(finish){ 
    del(['**/build', '**/obj', 'test/cordova/InAppTest'], function (err, deletedFiles) {
        console.log('Files deleted:', deletedFiles.join(', '));
        finish();
    });
});

/*
 * Copy cordova dependenties
 * Cordova plugins don't support symlinks so we have to copy some shared source files and libraries
 */
gulp.task('deps-cordova', function() {
    //ios
    gulp.src('src/atomic/ios/appstore/**/')
        .pipe(gulp.dest('src/cordova/ios/appstore/src/deps'));

    //android
    gulp.src('src/atomic/android/common/src/**/')
        .pipe(gulp.dest('src/cordova/android/common/src/deps'));

    gulp.src('src/atomic/android/googleplay/src/**/')
        .pipe(gulp.dest('src/cordova/android/googleplay/src/deps'));
    gulp.src('src/atomic/android/googleplay/aidl/**/')
        .pipe(gulp.dest('src/cordova/android/googleplay/src/deps'));

    gulp.src('src/atomic/android/amazon/src/**/')
        .pipe(gulp.dest('src/cordova/android/amazon/src/deps'));
    return gulp.src('src/atomic/android/amazon/libs/**/')
        .pipe(gulp.dest('src/cordova/android/amazon/src/deps'));
});

gulp.task('build-android', shell.task([
  'cd test/android/InAppTest && ./gradlew'
]));

gulp.task('build-ios', shell.task([
  'cd ./test/ios/InAppTest && xcodebuild'
]));

gulp.task('build-js', function () {
    return gulp.src('src/js/cocoon_inapps.js')
            .pipe(jshint())
            .pipe(jshint.reporter())
            .pipe(uglify())
            .pipe(gulp.dest('src/cordova/common/www'));
});

gulp.task('create-cordova', ['deps-cordova', 'build-js'], function(finish) {    

	var name = "InAppTest";
	var buildDir = path.join('test','cordova', name);
	var srcDir = path.join('test','cordova', "www");
	var appId = "com.ludei.basketgunner";
	var cfg = {lib: {www: {uri: srcDir, url: srcDir, link: false}}};
	console.log(buildDir);
	del([buildDir], function() {
		cordova.create(buildDir, appId, name, cfg)    
    	.then(function() {
        	process.chdir(buildDir);
    	})
    	.then(function() {
            console.log("Prepare cordova platforms");
        	return cordova.platform('add', ['android', 'ios']);
    	})
    	.then(function() {

            var plugins = [
                    "src/cordova/common",
                    "src/cordova/android/common",
                    "src/cordova/android/googleplay",
                    "src/cordova/ios/appstore"
                ];

            console.log("Add cordova plugins: " + JSON.stringify(plugins));

        	return cordova.plugins('add', plugins);
   		 })
        .then(function() {
            finish();
        })
        .fail(function(message) {
            console.error("Error: " + message);
            finish();
        })
	});
});

gulp.task('build-cordova', ['create-cordova'], function(finish) {

    cordova.build(['ios', 'android'])
    .then(function (){
        finish();
    })
    .fail(function(message){
        console.error(message);
        finish();
    });

});

gulp.task('build-cpp-ios', shell.task([
  'cd test/cpp/proj.ios && xcodebuild -target BuildDist'
]));

gulp.task('build-cpp-android', shell.task([
  'cd test/cpp/proj.android && ./gradlew assembleDebug' 
]));

gulp.task("build-cpp", ["build-cpp-ios", "build-cpp-android"]);

gulp.task("build", ["build-ios", "build-android", "build-cordova", "build-cpp"]);

gulp.task('doc-js', ["build-js"], function() {

    var config = require('./doc_template/js/jsdoc.conf.json');

    var infos = {
        plugins: config.plugins
    }

    var templates = config.templates;
    templates.path = 'doc_template/js';

    return gulp.src("src/js/*.js")
      .pipe(jsdoc.parser(infos))
      .pipe(jsdoc.generator('dist/doc/js', templates));

});

gulp.task('doc-android', shell.task([
  'cd ./doc_template/android && doxygen config'
]));

gulp.task('doc-ios', shell.task([
  'cd ./doc_template/ios && doxygen config'
]));

gulp.task('doc-cpp', shell.task([
  'cd ./doc_template/cpp && doxygen config'
]));

gulp.task('doc', ["doc-ios", "doc-android", "doc-js", "doc-cpp"]);

gulp.task('default', ['build', 'doc']);
