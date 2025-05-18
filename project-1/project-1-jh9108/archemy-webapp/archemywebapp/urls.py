"""archemy URL Configuration

The `urlpatterns` list routes URLs to views. For more information please see:
    https://docs.djangoproject.com/en/1.10/topics/http/urls/
Examples:
Function views
    1. Add an import:  from my_app import views
    2. Add a URL to urlpatterns:  url(r'^$', views.home, name='home')
Class-based views
    1. Add an import:  from other_app.views import Home
    2. Add a URL to urlpatterns:  url(r'^$', Home.as_view(), name='home')
Including another URLconf
    1. Import the include() function: from django.conf.urls import url, include
    2. Add a URL to urlpatterns:  url(r'^blog/', include('blog.urls'))
"""
from django.conf.urls import url
from . import views

urlpatterns = [
    url(r'^index/', views.index),
    url(r'^login/$', views.login),
    url(r'^logout/$', views.logout),
    url(r'^register/$', views.register_page),
    url(r'^register/success/$', views.register_success),
    url(r'^exploreCatalogue/$', views.ExploreCatalogue),
    url(r'^ArchDev/$', views.ArchDev),
    url(r'^AdaptiveReuse/$', views.AdaptiveReuse),
    url(r'^AEL/$', views.AEL),
    url(r'^AEL/ideation/$', views.AelIdeation),
    url(r'^AEL/inception/$', views.AelInception),
    url(r'^AEL/elaboration/$', views.AelElaboration),
    url(r'^AEL/implementation/$', views.AelImplementation),
    url(r'^AEL/deployment/$', views.AelDeployment),
    url(r'^AEL/operation/$', views.AelOperations),
]
