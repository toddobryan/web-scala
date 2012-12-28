getHighlight = () ->
    retLine = $('#editor-highlight .ace_text-layer').html()
    alert(retLine)
    $('#editor-highlight').remove()
    $('#prev-content').append(retLine)
    
postToAjax = (editor) -> 
    code = editor.getValue()
    highlightedCode = $('div.ace_text-layer').html()
    $.post('/interpret', $.param({line : code}), (result) ->
        $('#prev-content').append('<div>&gt; ' + highlightedCode + '</div> <br />')
        $('#prev-content').append(result + '<br /><br />')
    )
    alert("this alert doesn't run")
    editor.setValue("")

$('document').ready(() ->
    editor = ace.edit("editor")
    editor.setTheme("ace/theme/chrome")
    editor.getSession().setMode("ace/mode/scala")
    editor.setValue("")
    document.getElementById('editor').style.fontSize='16px';
    editor.commands.addCommand({
        name: 'myCommand',
        bindKey: {win: 'Ctrl-Return',  mac: 'Command-Return'},
        exec: (editor) -> postToAjax(editor)
    })
    editor.getSession().on('change', (e) ->
    	if(editor.session.getLength() < 10) 
    		$('div.ace_gutter').width(0)
    		$('div.ace_sb').width(0)
    		length = editor.session.getLength() * 18
    		$('div#editor').height(length)
    		editor.resize()
    	else 
    		$('div.ace_gutter').width(40)
    		$('div.ace_sb').width(20)
    )
    $('div.ace_gutter').width(0)
    $('div.ace_sb').width(0)
	$('#code').submit((e) ->
		postToAjax(editor)
		e.preventDefault()
	)
)