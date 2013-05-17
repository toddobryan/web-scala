goToInteractions = (editor) ->
	code = editor.getValue()
	filePath  = $('#info').attr('path')
	window.open('/ide/' + filePath, "Interactions", "width=800px,height=800px")
	
save = (editor, testEditor) ->
	code = editor.getValue()
	testCode = testEditor.getValue()
	alert(testCode)
	filePath = $('#info').attr('path')
	$.post('/save/' + filePath, $.param({content : code, test : testCode}), (result) -> 
		$('#message').css('display', 'block')
		$('#compiler-message').html("")
		$('#compiler-message').append(result)
		)
    
test = () ->
	filePath = $('#info').attr('path')
	window.open('/test/' + filePath, "Test Results", "width=800px,height=800px")

runTests = (editor) ->
	code = editor.getValue()
	filePath = $('#info').attr('path')
	window.open('/submitFile/' + filePath)
	
titleToggle = (aTitle) ->
	if($(aTitle).css('display') == "none") 
		$(aTitle).css('display', 'inline-block')
	else 
		$(aTitle).css('display', 'none')
	
toggleTest = () ->
	$('#test-form').toggle()
	$('#file').toggle()
	$('#title1').toggle()
	titleToggle('#title2')
    
toggleAlert = () ->
	$('#message').css('display', 'none')

$('document').ready(() ->
    editorWidth = window.innerWidth
    navbarHeight = $('div.navbar').height() + parseInt($('div.navbar').css('marginBottom'))
    formHeight = $('form.form-inline').height() + parseInt($('form.form-inline').css('marginBottom'))
    editorHeight = window.outerHeight - navbarHeight - formHeight
    ###
    Setting up the file editor.
    ###
    fileContent = $('#info').attr('fileContent')
    editor = ace.edit("editor")
    editor.setTheme("ace/theme/chrome")
    editor.getSession().setMode("ace/mode/scala")
    editor.setValue(fileContent)
    editor.clearSelection()
    #$('#editor').height(editorHeight)
    #$('#editor').width(editorWidth)
    #editor.resize()
    document.getElementById('editor').style.fontSize='12px';
    editor.commands.addCommand({
        name: 'myCommand',
        bindKey: {win: 'Ctrl-Return',  mac: 'Command-Return'},
        exec: (editor) -> compileWithAjax(editor)
    })
    $('#compileButton').click(() -> compileWithAjax(editor))
    $('#runTests').click(() -> runTests(editor))
    $('#enterInteractions').click(() -> goToInteractions(editor))
    ###
    Setting up test editor
    ###
    fileTests = $('#info').attr('fileTests')
    testeditor = ace.edit("test")
    testeditor.setTheme("ace/theme/chrome")
    testeditor.getSession().setMode("ace/mode/scala")
    testeditor.setValue(fileTests)
    testeditor.clearSelection()
    #$('div#test').height(editorHeight)
    #$('div#test').width(editorWidth)
    #testeditor.resize()
    document.getElementById('test').style.fontSize='12px';
    $('#testButton').click(() -> test())
    $('#saveButton').click(() -> save(editor, testeditor))
    $('#toggleTestButton').click(() -> toggleTest())
    $('#messageButton').click(() -> toggleAlert())
    $('#message').css('display', 'none')
)