from django.shortcuts import render, render_to_response
from django.contrib.auth import authenticate, login as auth_login, logout as auth_logout
from django.http import HttpResponse, HttpResponseRedirect
from django.contrib.auth.decorators import login_required
from django.views.decorators.csrf import csrf_protect, csrf_exempt
from django.template import RequestContext
from django.conf import settings
from archemywebapp.forms import *
from django.forms import *
import os
from subprocess import Popen,STDOUT,PIPE

# Create your views here.
@login_required
def index(request):
	return render(request, 'archemywebapp/index.html', {})

def login(request):
    next = request.GET.get('next', '/index/')
    if request.method == "POST":
        username = request.POST['username']
        password = request.POST['password']
        user = authenticate(username=username, password=password)

        if user is not None:
            if user.is_active:
                auth_login(request, user)
                return HttpResponseRedirect(next)
            else:
                return HttpResponse("Inactive user.")
        else:
            return HttpResponseRedirect(settings.LOGIN_URL)

    return render(request, "archemywebapp/login_view.html", {'redirect_to': next})

def logout(request):
    auth_logout(request)
    return HttpResponseRedirect('/login/')

def register_page(request):
    if request.method == 'POST':
        form = RegistrationForm(request.POST)
        if form.is_valid():
            user = User.objects.create_user(username=form.cleaned_data['username'],
                                            password=form.cleaned_data['password1'],
                                            email=form.cleaned_data['email'])
            args = ['java','-jar','./archemy-security-1.0-SNAPSHOT-jar-with-dependencies.jar','-cuser','-u',form.cleaned_data['username'],'-p',form.cleaned_data['password1'],'-r','NormalUser','-utype','normal']
            process = Popen(args, stdout=PIPE, stderr=STDOUT)
            for line in process.stdout:
                print(line)
            return HttpResponseRedirect('/register/success/')
    form = RegistrationForm()
    return render(request, 'archemywebapp/register_view.html', {'form': form})

def register_success(request):
    return render(request, 'archemywebapp/register_success.html')

@login_required
def ExploreCatalogue(request):
	return render(request, 'archemywebapp/ExploreCatalogue.html')

@login_required
def ArchDev(request):
	return render(request, 'archemywebapp/ArchDev.html', {})

@login_required
def AdaptiveReuse(request):
	return render(request, 'archemywebapp/AdaptiveReuse.html', {})

@login_required
def AEL(request):
	return render(request, 'archemywebapp/Ael.html', {})

@login_required
def AelIdeation(request):
	return render(request, 'archemywebapp/AelIdeation.html', {})

@login_required
def AelInception(request):
	return render(request, 'archemywebapp/AelInception.html', {})

@login_required
def AelElaboration(request):
	return render(request, 'archemywebapp/AelElaboration.html', {})

@login_required
def AelImplementation(request):
	return render(request, 'archemywebapp/AelImplementation.html', {})

@login_required
def AelDeployment(request):
	return render(request, 'archemywebapp/AelDeployment.html', {})

@login_required
def AelOperations(request):
	return render(request, 'archemywebapp/AelOperations.html', {})
