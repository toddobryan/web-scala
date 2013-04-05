saveWithAjax = (startEditor, testEditor) -> 
    startCode = startEditor.getValue()
    testCode = testEditor.getValue()
    blockTitle = $('#info').attr('block')
    assignTitle = $('#info').attr('title')
    $.post('/myClasses/' + blockTitle + '/editAssignment/' + assignTitle, $.param({start : startCode, test : testCode}), (result) -> 
    	$('#prev-content').html("")
    	$('#compiler-message').html("")
    	$('#compiler-message').append(result)
    	)

$('document').ready(() ->
    ###
    Setting up the starter editor.
    ###
    startContent = $('#info').attr('start')
    testContent = $('#info').attr('test')
    starterEditor = ace.edit("starter")
    starterEditor.setTheme("ace/theme/chrome")
    starterEditor.getSession().setMode("ace/mode/scala")
    starterEditor.setValue(startContent)
    starterEditor.clearSelection()
    document.getElementById('starter').style.fontSize='20px';
    testEditor = ace.edit("test")
    testEditor.setTheme("ace/theme/chrome")
    testEditor.getSession().setMode("ace/mode/scala")
    testEditor.setValue(testContent)
    testEditor.clearSelection()
    document.getElementById('test').style.fontSize='20px';
    $('#saveButton').click(() -> saveWithAjax(starterEditor, testEditor))
    )