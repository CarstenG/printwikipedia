#!/usr/bin/env python
# -*- coding: utf-8 -*-
from selenium import webdriver 
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.common.keys import Keys
from PIL import Image

import selenium
import os
import urllib
import time
import re
execfile('pass.py')

global pwd
global outFolder
global inFolder
global browser
pwd = os.path.abspath(os.getcwd())
outFolder= pwd+"/out"
inFolder= pwd+"/in"
#open file and read what last char is. if 0 not finished so find last entry in line and then use that one
#other chars mean different things but for right now we'll do this

def immigration(volumeNum,inputFile,browser):
	browser.quit()
	print "immigration office. moving volume "+ volumeNum
	for i in os.listdir(inFolder):
		if inputFile == i:
			#move that file to out and then find the next one using volumeNum
			os.rename(inFolder+"/"+inputFile, outFolder+"/"+inputFile)
			break
	nextCheck=int(volumeNum)+1#convert to int and add one to find next volume
	#make list and then use .sort() to get through all of them save it elsewhere. and keep pulling the last one out.
	for i in os.listdir(inFolder):#find the next inputFile
		if i == '.DS_Store':
			print "ignore .ds_store"
			continue
		splitFile = i.split('-')
		print splitFile
		nextDest = int(splitFile[1])#also change that to an int to compare with nextcheck
		print nextDest
		print nextCheck
		if nextCheck == nextDest:
			print "found it"
			print i
			travelAgent(i)
			break

def elemWait(waitTime,waitElement,browser): #this is a certain kind of wait (uploads and flash loading screens lulu likes)
	try:
		WebDriverWait(browser, waitTime).until(EC.presence_of_element_located((By.XPATH, waitElement)))
		return True
	except Exception:
		return False
def execution(browser,waitElement,waitTime,ex_type):#called whenever interacting with an element
	if ex_type=="click":
		if elemWait(waitTime,waitElement,browser) == True:
			return browser.find_element_by_xpath(waitElement).click()
		else:
			print "element @ " +waitElement+ " could not be found."
	elif ex_type=="text":
		if elemWait(waitTime,waitElement,browser) == True:
			return browser.find_element_by_xpath(waitElement)
		else:
			print "element @ " +waitElement+ " could not be found."
	else:
		print "something went wrong here"
		return False
def iterateFiles(inputFile, browser, encoding="utf-8"):
	print "about to go through files"
	print "get in iframe"
	browser.switch_to_frame(browser.find_element_by_tag_name("iframe"))
	numXpath = "/html/body/div/div/table/tbody/tr/td[2]/div/div/div/div/span[2]"
	submitButton = "/html/body/div/div/table/tbody/tr/td[2]/div/div/table/tbody/tr/td/input"
	r_numOfPages = execution(browser,numXpath,10,"text")
	numOfPages = r_numOfPages.text
	x=0
	exitLoop = False
	#row of table with title
	#/html/body/div/div/table/tbody/tr/td[2]/div/div[3]/div/form/div/div/div[2]/table/tbody/tr[2]/td[2]/span
	while x<numOfPages and exitLoop is False:
		for i in range(0,24):
			if i == 0:
				xFile = "/html/body/div/div/table/tbody/tr/td[2]/div/div[3]/div/form/div/div/div[2]/table/tbody/tr/td[2]/span"
				"/html/body/div[1]/div/table/tbody/tr/td[2]/div/div[3]/div[1]/form/div/div/div[2]/table/tbody/tr[1]/td[1]/input"
			else:
				xFile = "/html/body/div/div/table/tbody/tr/td[2]/div/div[3]/div/form/div/div/div[2]/table/tbody/tr["+str(i)+"]/td[2]/span"
				r_isFile = execution(browser,xFile,3,"text")
				isFile = r_isFile.text
				print i
				print isFile
				# isFile = isFile.decode('utf-8', "ignore")
				inputFile = inputFile.decode('utf-8', 'ignore')
				print "input "+str(type(inputFile)) + " isfile " + str(type(isFile))

				# isFile = isFile.decode('utf-8')
				if isFile == inputFile:
					print "~found your file~"
					checkBox = xFile[:-8] #this is the checkbox which is just one td over so we chop off the end of the xfile path
					print checkBox
					checkBox = checkBox + "[1]/input"
					execution(browser,checkBox,3,"click")
					exitLoop = True
					break
					
			i+=1
		x+=1
		xNext = "/html/body/div/div/table/tbody/tr/td[2]/div/div/div/div/div/a[8]"
		execution(browser,xNext,300,"click")
	execution(browser,submitButton,400,"click")#click submit
	browser.switch_to_default_content()#get out of iframe
	# WebDriverWait(browser, 3500).until(EC.presence_of_element_located((By.XPATH, "//*[@id='fNext' and not (@disabled)]")))
	execution(browser,"//*[@id='fNext' and not (@disabled)]",350,"click")



def existingText(tElement,changeTo):#this function deletes textarea or text input fields. For whatever reason .clear() was not working
	#tElement==the text box.
	#changeTo is the text you wish to input
	for n in range(0,20):
		tElement.send_keys("", Keys.ARROW_RIGHT)
	for n in range(0,20):
		tElement.send_keys("", Keys.BACK_SPACE)
	tElement.send_keys(changeTo)

def luluCruise(inputFile,volumeNum,title):
	print "opening browser for a cool lulu cruise with: " + title + " on Firefox"
	browser = webdriver.Firefox()
	browser.get("http://www.lulu.com/author/wizard/index.php?fWizard=hardcover")#change to author page
	#use firefox and go to the login page
	uid = "//*[@id='loginEmail']" #id/name for logging in
	pw = "//*[@id='loginPassword']" #password area
	logbutt = "//*[@id='loginSubmit']" #submit button
	#send keys and click login to get to the home page.
	r_uid = execution(browser,uid,15,"text")
	r_uid.send_keys(lulu_email)
	r_pw = execution(browser,pw,15,"text")
	r_pw.send_keys(lulu_pass)
	execution(browser,logbutt,25,"click") #submit by clicking
	print "successfully logged in, now get to creating the book"
	print "options"
	serviceType = "//*[@id='productline_3']"
	bookSize = "//*[@id='preset_1037_73']"
	binding = "//*[@id='binding_4']"
	pgCount = "//*[@id='pagecount']"
	cont2 = "//*[@id='fNext']"
	myPage = "700"
	execution(browser,serviceType,20,"click")
	execution(browser,serviceType,20,"click")
	
	# browser.find_element_by_xpath("/html/body/div/div[3]/div[2]/div[2]/div/div/div/div/a[3]/img").click()
	# browser.find_element_by_xpath("/html/body/div/div[3]/div[2]/div[2]/div/div/div/div/a[3]/span").click()
	execution(browser,bookSize,20,"click")
	existingText(execution(browser,pgCount,10,"text"),myPage)#clear this thing and put in 700
	execution(browser,binding,10,"click")
	execution(browser,cont2,10,"click")
	print "title and author page"
	bookTitle = "//*[@id='title']"#get and put title
	existingText(execution(browser,bookTitle,10,"text"),title)
	cont1 = browser.find_element_by_xpath("//*[@id='fNext']")
	cont1.click()
	print "get to isbn page"
	getISBN = browser.find_element_by_xpath("//*[@id='fNext']")#should be set to default by browser on isbn page to get a new one
	getISBN.click()
	print "dl pdf on this page"
	# pdfLink = browser.find_element_by_xpath("//*[@id='BarcodeImage']").get_attribute('href')
	bcImage = browser.find_element_by_xpath("/html/body/div/div[3]/div[2]/div[2]/div/div[2]/div[4]/a/img")
	location = bcImage.location
	size = bcImage.size
	print "taking screenshot"
	browser.save_screenshot('barcode.png')
	print "croping screen screenshot"
	im = Image.open('barcode.png')
	left = location['x']
	top = location['y']
	right = location['x'] + size['width']
	bottom = location['y'] + size['height']
	im = im.crop((left, top, right, bottom)) # defines crop points
	im.save('barcode.png') # saves new cropped image
	print "all done it's in barcode.png"

	cont3 = browser.find_element_by_xpath("//*[@id='fNext']")
	cont3.click()

	print "tada now time for uploading."
	myFiles = "//*[@id='ui-id-2']"
	execution(browser,myFiles,10,"click")#access the other part here with the list of files that you should have FTP'd in
	iterateFiles(inputFile, browser)
	print "file selected and found soundly"
	# browser.find_element_by_xpath("//*[@id='ui-id-2']").click()
	# uploadFile = inFolder +"/"+inputFile
	# print uploadFile
	# browser.find_element_by_xpath("//*[@id='uploadField']").send_keys(unicode(uploadFile, 'utf-8'))
	# cont4 = browser.find_element_by_xpath("//*[@id='fMegaUpload']") #click this and upload
	# cont4.click()
	# print "wait for upload"
	# uploadButt = browser.find_element_by_xpath("//*[@id='fNext']").get_attribute('class')
	# #while this button is still unclickable keep waiting. but also check to see if there are errors.
	print "i'm clicking this to go next"
	
	# elemWait(350, "//*[@id='fNext']",browser)

	print "wait for animation and unecessary cover upload page to load..."
	execution(browser,cont2,350,"click")
	# elemWait(120,"//*[@id='fNext']",browser)

	print "want to use the old one-piece cover page'"
	browser.find_element_by_xpath("/html/body/div[1]/div[5]/div/div[2]/a[2]").click()
	# execution(browser,"//*[@id='OPCbtn']",350,"click")
	# elemWait(120,"//*[@id='OPCbtn']",browser) #weird loading page
	# coverUpload = browser.find_element_by_xpath("//*[@id='fOnePieceCoverFile']")#upload cover
	print "uploading cover"
	coverUpload = "//*[@id='fOnePieceCoverFile']"
	r_coverUpload = execution(browser,coverUpload,350,"text")
	r_coverUpload.send_keys(pwd +"/"+"newCover.jpg")
	browser.find_element_by_xpath("//*[@id='fMegaUpload']").click()
	print "wait for upload to complete"
	execution(browser,"//*[@id='fNext' and not (@disabled)]",350,"click")
	# elemWait(330,"//*[@id='fNext']",browser)

	catSelect=browser.find_element_by_xpath("//*[@id='category']")
	catOptions = catSelect.find_elements_by_tag_name("option")
	for option in catOptions:
		if option.text=="Reference":
			print "found reference option in select"
			option.click()
			break
	print "keywords"
	keywords = browser.find_element_by_xpath("//*[@id='keywords']")
	keywords.send_keys("poetry, reference, wiki, mandiberg")
	description = browser.find_element_by_xpath("//*[@id='descr']")
	description.send_keys("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx must be 50 chars long")
	#here for language it would be funny to say it's in javanese
	#because of java
	#right?
	#riight?
	#automatically set to english
	copyrightInfo = browser.find_element_by_xpath("//*[@id='copyright']")
	copyrightInfo.send_keys("Michael Mandiberg")
	#i think this should be for mandiberg?

	lisenceSelect = browser.find_element_by_xpath("//*[@id='license']")
	liscOptions = lisenceSelect.find_elements_by_tag_name("option")
	for option in liscOptions:
		if option.text=="Creative Commons Attribution-ShareAlike 2.0":
			option.click()
			break
	editionNum = browser.find_element_by_xpath("//*[@id='edition']")
	editionNum.send_keys("01")
	publisher = browser.find_element_by_xpath("//*[@id='publisher']")
	publisher.send_keys("bad boy records")
	browser.find_element_by_xpath("//*[@id='fNext']").click()
	print "set your price page"
	# setPrice = "//*[@id='userPrice_11']"
	# myPrice="32.97"
	# r_setPrice = execution(browser,setPrice,350,"text")
	# existingText(r_setPrice,myPrice)#clear and input new price.
	browser.find_element_by_xpath("//*[@id='fNext']").click()
	print "almost there. just lemme review the order here"
	browser.find_element_by_xpath("//*[@id='fNext']").click()
	print "book " +title+ " pushed to lulu okay."
	print "begin next book at vol #"+volumeNum
	immigration(volumeNum, inputFile, browser)

def travelAgent(inputFile):
	print "travel agent: splitting strings, encoding for unicode, sending off on your cruise for file " + inputFile
	splitInput = inputFile.split('-')#split on the dash symbol. to make a nice buncha strings
	volumeNum = splitInput[1]#this is the volume number we are currently on. 
	title = splitInput[2]+" --- "+splitInput[3]
	title=unicode(title, 'utf-8')#encode that real nice for lulu
	luluCruise(inputFile,volumeNum,title)


travelAgent("Vol-00001-bb-symbol-10215110-4374729 (copy).pdf")
